package models.event

import java.time.LocalDateTime

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
  def time: LocalDateTime

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

    /**
      * Job is in progress.
      */
    case object InProgress extends Status
  }

  /**
    * Upload event report to google drive.
    */
  case class Upload(id: Long, eventId: Long, time: LocalDateTime, status: Status) extends EventJob {
    override def copyWith(id: Long = id, eventId: Long = eventId, status: EventJob.Status = status): EventJob = {
      copy(id = id, eventId = eventId, status = status)
    }
  }

  /**
    * Send event notifications to email.
    */
  case class SendNotification(id: Long, eventId: Long, notification: Event.NotificationTime, status: Status)
    extends EventJob {
    override def copyWith(id: Long = id, eventId: Long = eventId, status: EventJob.Status = status): EventJob = {
      copy(id = id, eventId = eventId, status = status)
    }
    override def time: LocalDateTime = notification.time
  }

  /**
    * Event start.
    */
  case class EventStart(id: Long, eventId: Long, time: LocalDateTime, status: Status) extends EventJob {
    override def copyWith(id: Long = id, eventId: Long = eventId, status: EventJob.Status = status): EventJob = {
      copy(id = id, eventId = eventId, status = status)
    }
  }
}
