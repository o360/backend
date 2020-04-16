/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services.event

import java.time.LocalDateTime

import javax.inject.{Inject, Singleton}
import models.dao.{EventDao, EventJobDao}
import models.event.{Event, EventJob}
import services.{NotificationService, ServiceResults, UploadService}
import utils.errors.NotFoundError
import utils.implicits.FutureLifting._
import utils.{Logger, TimestampConverter}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Event job service.
  */
@Singleton
class EventJobService @Inject() (
  protected val eventJobDao: EventJobDao,
  protected val eventDao: EventDao,
  protected val notificationService: NotificationService,
  protected val uploadService: UploadService,
  protected val eventStartService: EventStartService,
  implicit val ec: ExecutionContext
) extends Logger
  with ServiceResults[EventJob] {

  def runFailedJob(id: Long): UnitResult = {
    for {
      job <- eventJobDao.find(id).liftRight {
        NotFoundError.EventJob(id)
      }

      _ <- ensure(job.status == EventJob.Status.Failure) {
        NotFoundError.EventJob(id)
      }

      _ = execute(Seq(job))
    } yield ()
  }

  /**
    * Creates jobs for event.
    */
  def createJobs(event: Event): Future[Unit] = {
    val uploadJob = EventJob.Upload(0, event.id, event.end, EventJob.Status.New)
    val sendEmailJobs = event.notifications.map(EventJob.SendNotification(0, event.id, _, EventJob.Status.New))
    val eventStart = EventJob.EventStart(0, event.id, event.start, EventJob.Status.New)
    val jobsInTheFuture = (eventStart +: uploadJob +: sendEmailJobs)
      .filter(_.time.isAfter(TimestampConverter.now))
    Future.sequence(jobsInTheFuture.map(eventJobDao.createJob(_))).map(_ => ())
  }

  /**
    * Returns event jobs in new status from given time interval.
    */
  def get(from: LocalDateTime, to: LocalDateTime): Future[Seq[EventJob]] = {

    /**
      * Splits collection using future of predicate.
      *
      * @param seq collection
      * @param splitter future of predicate
      * @return future of tuple (elements that matched predicate; elements that not matched predicate)
      */
    def split[A](seq: Seq[A], splitter: A => Future[Boolean]): Future[(Seq[A], Seq[A])] = {
      Future
        .sequence {
          seq.map(el => splitter(el).map((el, _)))
        }
        .map { result =>
          val positive = result.collect { case (el, cond) if cond  => el }
          val negative = result.collect { case (el, cond) if !cond => el }

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
            case j: EventJob.Upload           => event.end == j.time
            case j: EventJob.SendNotification => event.notifications.contains(j.notification)
            case j: EventJob.EventStart       => event.start == j.time
          }
        case None =>
          false
      }
    }

    for {
      jobs <- eventJobDao.getJobs(from, to, EventJob.Status.New)
      (actualJobs, cancelledJobs) <- split(jobs, isStillActual)
      _ <- updateStatus(cancelledJobs, EventJob.Status.Cancelled)
    } yield actualJobs
  }

  /**
    * Executes given jobs.
    */
  def execute(jobs: Seq[EventJob]): Unit = {
    def executeJob(job: EventJob): Unit = {
      val result = job match {
        case j: EventJob.Upload           => uploadService.execute(j)
        case j: EventJob.SendNotification => notificationService.execute(j)
        case j: EventJob.EventStart       => eventStartService.execute(j)
      }

      result.onComplete {
        case Success(_) =>
          log.debug(s"Event job $job completed successfully")
          eventJobDao.updateStatus(job.id, EventJob.Status.Success)
        case Failure(e) =>
          log.error(s"Attempt for the job: $job failed", e)
          eventJobDao.updateStatus(job.id, EventJob.Status.Failure)
      }
    }
    updateStatus(jobs, EventJob.Status.InProgress).map { _ =>
      jobs.foreach(executeJob)
    }
  }

  /**
    * Updates status in jobs.
    */
  def updateStatus(jobs: Seq[EventJob], status: EventJob.Status): Future[Unit] = {
    Future.sequence(jobs.map(x => eventJobDao.updateStatus(x.id, status))).map(_ => ())
  }
}
