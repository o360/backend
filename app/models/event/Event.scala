package models.event

import java.sql.Timestamp
import java.time.ZoneOffset

import models.NamedEntity
import models.notification.Notification
import utils.TimestampConverter

/**
  * Event model.
  *
  * @param id            DB ID
  * @param description   description
  * @param start         start date-time
  * @param end           end date-time
  * @param notifications collection of notification types along with time and recipient type
  */
case class Event(
  id: Long,
  description: Option[String],
  start: Timestamp,
  end: Timestamp,
  notifications: Seq[Event.NotificationTime]
) {
  private val currentTime = TimestampConverter.now

  /**
    * Status of event.
    */
  val status: Event.Status =
    if (currentTime.before(start)) Event.Status.NotStarted
    else if (currentTime.before(end)) Event.Status.InProgress
    else Event.Status.Completed

  /**
    * Event text representation.
    */
  def caption(zone: ZoneOffset): String = s"Event ${description.map(_ + " ").getOrElse("")}" +
    s"(${TimestampConverter.toPrettyString(start, zone)} - ${TimestampConverter.toPrettyString(end, zone)})"

  def toNamedEntity(zone: ZoneOffset) = NamedEntity(id, caption(zone))
}

object Event {
  val namePlural = "events"

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
