package models.dao

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import models.event.{Event, EventJob}
import models.notification.Notification
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
  * Component for event_job table.
  */
trait EventJobComponent extends NotificationComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  implicit val jobStatusColumnType = MappedColumnType.base[EventJob.Status, Byte](
    {
      case EventJob.Status.New => 0
      case EventJob.Status.Success => 1
      case EventJob.Status.Failure => 2
      case EventJob.Status.Cancelled => 3
      case EventJob.Status.InProgress => 4
    }, {
      case 0 => EventJob.Status.New
      case 1 => EventJob.Status.Success
      case 2 => EventJob.Status.Failure
      case 3 => EventJob.Status.Cancelled
      case 4 => EventJob.Status.InProgress
    }
  )

  /**
    * Event job DB model.
    */
  case class DbEventJob(
    id: Long,
    eventId: Long,
    time: Timestamp,
    status: EventJob.Status,
    kind: Option[Notification.Kind],
    recipient: Option[Notification.Recipient],
    jobType: Byte
  ) {
    def toModel: EventJob = jobType match {
      case 0 =>
        EventJob.Upload(id, eventId, time, status)
      case 1 =>
        val notification = Event.NotificationTime(
          time,
          kind.getOrElse(throw new NoSuchElementException(s"missed kind in notification job for event $eventId")),
          recipient.getOrElse(
            throw new NoSuchElementException(s"missed recipient in notification job for event $eventId"))
        )
        EventJob.SendNotification(id, eventId, notification, status)
      case 2 =>
        EventJob.CreateFreezedForms(id, eventId, time, status)
    }
  }

  object DbEventJob {
    def fromModel(job: EventJob) = job match {
      case j: EventJob.Upload =>
        DbEventJob(j.id, j.eventId, j.time, j.status, None, None, 0)
      case j: EventJob.SendNotification =>
        DbEventJob(j.id, j.eventId, j.time, j.status, Some(j.notification.kind), Some(j.notification.recipient), 1)
      case j: EventJob.CreateFreezedForms =>
        DbEventJob(j.id, j.eventId, j.time, j.status, None, None, 2)
    }
  }

  class EventJobTable(tag: Tag) extends Table[DbEventJob](tag, "event_job") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def eventId = column[Long]("event_id")
    def time = column[Timestamp]("time")
    def status = column[EventJob.Status]("status")
    def kind = column[Option[Notification.Kind]]("notification_kind")
    def recipient = column[Option[Notification.Recipient]]("notification_recipient_kind")
    def jobType = column[Byte]("job_type")

    def * = (id, eventId, time, status, kind, recipient, jobType) <> ((DbEventJob.apply _).tupled, DbEventJob.unapply)
  }

  val EventJobs = TableQuery[EventJobTable]
}

/**
  * Event job DAO.
  */
@Singleton
class EventJobDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
  with EventJobComponent
  with DaoHelper {

  import driver.api._

  /**
    * Creates job in DB if not exists.
    */
  def createJob(job: EventJob): Future[Unit] = {
    val dbJob = DbEventJob.fromModel(job)
    val existedJobQuery = EventJobs
      .filter { x =>
        x.eventId === dbJob.eventId &&
        x.time === dbJob.time &&
        dbJob.kind.fold(x.kind.isEmpty)(k => x.kind.fold(false: Rep[Boolean])(_ === k)) &&
        dbJob.recipient.fold(x.kind.isEmpty)(r => x.recipient.fold(false: Rep[Boolean])(_ === r)) &&
        x.jobType === dbJob.jobType
      }
      .map(_.id)
      .result
      .headOption

    for {
      existingJobId <- db.run(existedJobQuery)
      _ <- existingJobId match {
        case Some(id) => db.run(EventJobs.filter(_.id === id).update(dbJob.copy(id = id)))
        case None => db.run(EventJobs += dbJob)
      }
    } yield ()
  }

  /**
    * Updates status.
    */
  def updateStatus(jobId: Long, status: EventJob.Status): Future[Unit] =
    db.run {
        EventJobs
          .filter(_.id === jobId)
          .map(_.status)
          .update(status)
      }
      .map(_ => ())

  /**
    * Return jobs filtered by given criteria.
    */
  def getJobs(from: Timestamp, to: Timestamp, status: EventJob.Status): Future[Seq[EventJob]] = {
    val query = EventJobs
      .filter(x => x.time > from && x.time <= to && x.status === status)
      .result
      .map(_.map(_.toModel))

    db.run(query)
  }

  /**
    * Returns event job by ID.
    */
  def find(id: Long): Future[Option[EventJob]] = {
    db.run {
      EventJobs
        .filter(_.id === id)
        .result
        .map(_.headOption.map(_.toModel))
    }
  }

}
