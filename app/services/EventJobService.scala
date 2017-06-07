package services

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import models.dao.{EventDao, EventJobDao}
import models.event.{Event, EventJob}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import utils.{Logger, TimestampConverter}

import scala.util.{Failure, Success}

/**
  * Event job service.
  */
@Singleton
class EventJobService @Inject()(
  protected val eventJobDao: EventJobDao,
  protected val eventDao: EventDao,
  protected val notificationService: NotificationService,
  protected val uploadService: UploadService
) extends Logger {

  /**
    * Creates jobs for event.
    */
  def createJobs(event: Event): Future[Unit] = {
    val uploadJob = EventJob.Upload(0, event.id, event.end, EventJob.Status.New)
    val sendEmailJobs = event.notifications.map(EventJob.SendNotification(0, event.id, _, EventJob.Status.New))
    val jobsInTheFuture = (uploadJob +: sendEmailJobs).filter(_.time.after(TimestampConverter.now))
    Future.sequence(jobsInTheFuture.map(eventJobDao.createJob(_))).map(_ => ())
  }

  /**
    * Returns event jobs in new status from given time interval.
    */
  def get(from: Timestamp, to: Timestamp): Future[Seq[EventJob]] = {

    /**
      * Splits collection using future of predicate.
      *
      * @param seq collection
      * @param splitter future of predicate
      * @return future of tuple (elements that matched predicate; elements that not matched predicate)
      */
    def split[A](seq: Seq[A], splitter: A => Future[Boolean]): Future[(Seq[A], Seq[A])] = {
      Future.sequence {
        seq.map(el => splitter(el).map((el, _)))
      }.map { result =>
        val positive = result.filter(_._2).map(_._1)
        val negative = result.filter(!_._2).map(_._1)
        (positive, negative)
      }
    }

    /**
      * Checks if current event state matches job.
      */
    def isStillActual(job: EventJob) = {
      eventDao.findById(job.eventId).map {
        case Some(event) =>
          job match {
            case j: EventJob.Upload => event.end == j.time
            case j: EventJob.SendNotification => event.notifications.contains(j.notification)
          }
        case None =>
          false
      }
    }


    for {
      jobs <- eventJobDao.getJobs(from, to, EventJob.Status.New)
      (actualJobs, cancelledJobs) <- split(jobs, isStillActual)
      _ <- cancel(cancelledJobs)
    } yield actualJobs
  }

  /**
    * Executes given jobs.
    */
  def execute(jobs: Seq[EventJob]): Unit = {
    def executeJob(job: EventJob) = {
      val result = job match {
        case j: EventJob.Upload => uploadService.execute(j)
        case j: EventJob.SendNotification => notificationService.execute(j)
      }

      result.onComplete {
        case Success(_) =>
          eventJobDao.updateStatus(job.id, EventJob.Status.Success)
        case Failure(e) =>
          log.error(s"Event id:${job.eventId} job failed", e)
          eventJobDao.updateStatus(job.id, EventJob.Status.Failure)
      }
    }

    jobs.foreach(executeJob)
  }

  /**
    * Cancels given jobs.
    */
  def cancel(jobs: Seq[EventJob]): Future[Unit] = {
    Future.sequence(jobs.map(x => eventJobDao.updateStatus(x.id, EventJob.Status.Cancelled))).map(_ => ())
  }
}
