package models.event

import java.sql.Timestamp

/**
  * Job for event.
  */
trait EventJob {
  /**
    * Job ID.
    */
  def id: Long

  /**
    * Fire time.
    */
  def time: Timestamp
  /**
    * Id of event.
    */
  def eventId: Long

  /**
    * Job status.
    */
  def status: EventJob.Status

  /**
    * Similar to case class copy method.
    */
  def copyWith(id: Long = id, eventId: Long = eventId, status: EventJob.Status = status): EventJob
}

object EventJob {

  /**
    * Job status.
    */
  trait Status
  object Status {
    /**
      * New job.
      */
    case object New extends Status
    /**
      * Job completed successfully.
      */
    case object Success extends Status
    /**
      * Job completed with failure.
      */
    case object Failure extends Status
    /**
      * Job cancelled.
      */
    case object Cancelled extends Status
  }

  /**
    * Upload event report to google drive.
    */
  case class Upload(id: Long, eventId: Long, time: Timestamp, status: Status) extends EventJob {
    override def copyWith(id: Long = id, eventId: Long = eventId, status: EventJob.Status = status): EventJob =
      copy(id = id, eventId = eventId, status = status)
  }

  /**
    * Send event notifications to email.
    */
  case class SendNotification(id: Long, eventId: Long, notification: Event.NotificationTime, status: Status) extends EventJob {
    override def copyWith(id: Long = id, eventId: Long = eventId, status: EventJob.Status = status): EventJob =
      copy(id = id, eventId = eventId, status = status)
    override def time: Timestamp = notification.time
  }

  /**
    * Create freezed forms for event.
    */
  case class CreateFreezedForms(id: Long, eventId: Long, time: Timestamp, status: Status) extends EventJob {
    override def copyWith(id: Long = id, eventId: Long = eventId, status: EventJob.Status = status): EventJob =
      copy(id = id, eventId = eventId, status = status)
  }
}
