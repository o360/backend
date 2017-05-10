package models.event

import java.sql.Timestamp

import models.notification.Notification

/**
  * Event model.
  *
  * @param id            DB ID
  * @param description   description
  * @param start         start date-time
  * @param end           end date-time
  * @param canRevote     if true, user can revote
  * @param notifications collection of notification types along with time and recipient type
  */
case class Event(
  id: Long,
  description: Option[String],
  start: Timestamp,
  end: Timestamp,
  canRevote: Boolean,
  notifications: Seq[Event.NotificationTime]
) {
  private val currentTime = new Timestamp(System.currentTimeMillis)

  /**
    * Status of event.
    */
  val status: Event.Status =
    if (currentTime.before(start)) Event.Status.NotStarted
    else if (currentTime.before(end)) Event.Status.InProgress
    else Event.Status.Completed
}

object Event {
  /**
    * Event notification time.
    *
    * @param time      time to send notification
    * @param kind      notification kind
    * @param recipient notification recipient kind
    */
  case class NotificationTime(
    time: Timestamp,
    kind: Notification.Kind,
    recipient: Notification.Recipient
  )

  /**
    * Event status.
    */
  sealed trait Status
  object Status {
    /**
      * Event starts in the future.
      */
    case object NotStarted extends Status
    /**
      * Event is in progress.
      */
    case object InProgress extends Status
    /**
      * Event completed.
      */
    case object Completed extends Status
  }
}
