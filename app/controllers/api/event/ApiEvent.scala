package controllers.api.event

import java.sql.Timestamp

import controllers.api.{EnumFormat, EnumFormatHelper, Response}
import models.event.Event
import play.api.libs.json.Json
import controllers.api.TimestampFormat._
import controllers.api.notification.{ApiNotificationKind, ApiNotificationRecipient}
import models.user.User

/**
  * Api model for event.
  */
case class ApiEvent(
  id: Long,
  description: Option[String],
  start: Timestamp,
  end: Timestamp,
  canRevote: Boolean,
  notifications: Option[Seq[ApiEvent.NotificationTime]],
  status: ApiEvent.EventStatus
) extends Response

object ApiEvent {

  /**
    * Creates api model form model.
    */
  def apply(e: Event)(implicit account: User): ApiEvent = {
    val notifications = account.role match {
      case User.Role.Admin => Some(e.notifications.map(NotificationTime(_)))
      case User.Role.User => None
    }
    
    ApiEvent(
      e.id,
      e.description,
      e.start,
      e.end,
      e.canRevote,
      notifications,
      EventStatus(e.status)
    )
  }

  implicit val notificationTimeFormat = Json.format[NotificationTime]
  implicit val eventWrites = Json.writes[ApiEvent]

  /**
    * Notification time api model.
    */
  case class NotificationTime(
    time: Timestamp,
    kind: ApiNotificationKind,
    recipient: ApiNotificationRecipient
  ){
    /**
      * Converts api model to model.
      */
    def toModel: Event.NotificationTime = Event.NotificationTime(
      time,
      kind.value,
      recipient.value
    )
  }

  object NotificationTime {
    /**
      * Creates api model from model.
      */
    def apply(n: Event.NotificationTime): NotificationTime = NotificationTime(
      n.time,
      ApiNotificationKind(n.kind),
      ApiNotificationRecipient(n.recipient)
    )
  }

  /**
    * Event status.
    */
  case class EventStatus(value: Event.Status) extends EnumFormat[Event.Status]
  object EventStatus extends EnumFormatHelper[Event.Status, EventStatus]("event status") {

    import Event.Status._

    override protected def mapping: Map[String, Event.Status] = Map(
      "notStarted" -> NotStarted,
      "inProgress" -> InProgress,
      "completed" -> Completed
    )
  }
}
