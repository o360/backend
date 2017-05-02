package controllers.api.event

import java.sql.Timestamp

import controllers.api.{EnumFormat, EnumFormatHelper, Response}
import models.event.Event
import models.notification.Notification
import play.api.libs.json.Json
import controllers.api.TimestampFormat._

/**
  * Api model for event.
  */
case class ApiEvent(
  id: Long,
  description: Option[String],
  start: Timestamp,
  end: Timestamp,
  canRevote: Boolean,
  notifications: Seq[ApiEvent.NotificationTime],
  status: ApiEvent.EventStatus
) extends Response

object ApiEvent {

  /**
    * Creates api model form model.
    */
  def apply(e: Event): ApiEvent = ApiEvent(
    e.id,
    e.description,
    e.start,
    e.end,
    e.canRevote,
    e.notifications.map(NotificationTime(_)),
    EventStatus(e.status)
  )

  implicit val notificationTimeFormat = Json.format[NotificationTime]
  implicit val eventWrites = Json.writes[ApiEvent]

  /**
    * Notification time api model.
    */
  case class NotificationTime(
    time: Timestamp,
    kind: NotificationKind,
    recipient: NotificationRecipient
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
      NotificationKind(n.kind),
      NotificationRecipient(n.recipient)
    )
  }

  /**
    * Kind of notification.
    */
  case class NotificationKind(value: Notification.Kind) extends EnumFormat[Notification.Kind]
  object NotificationKind extends EnumFormatHelper[Notification.Kind, NotificationKind]("notification kind") {

    import Notification.Kind._

    override protected def mapping: Map[String, Notification.Kind] = Map(
      "preBegin" -> PreBegin,
      "begin" -> Begin,
      "preEnd" -> PreEnd,
      "end" -> End
    )
  }

  /**
    * Kind of notification recipient.
    */
  case class NotificationRecipient(value: Notification.Recipient) extends EnumFormat[Notification.Recipient]
  object NotificationRecipient
    extends EnumFormatHelper[Notification.Recipient, NotificationRecipient]("notification recipient") {

    import Notification.Recipient._

    override protected def mapping: Map[String, Notification.Recipient] = Map(
      "respondent" -> Respondent,
      "auditor" -> Auditor
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
