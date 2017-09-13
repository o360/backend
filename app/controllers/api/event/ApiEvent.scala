package controllers.api.event

import java.sql.Timestamp
import java.time.LocalDateTime

import controllers.api.{EnumFormat, EnumFormatHelper, Response}
import models.event.Event
import play.api.libs.json.Json
import controllers.api.notification.{ApiNotificationKind, ApiNotificationRecipient}
import models.user.User
import utils.TimestampConverter

/**
  * Api model for event.
  */
case class ApiEvent(
  id: Long,
  description: Option[String],
  start: LocalDateTime,
  end: LocalDateTime,
  notifications: Option[Seq[ApiEvent.NotificationTime]],
  status: ApiEvent.EventStatus,
  userInfo: Option[ApiEvent.ApiUserInfo]
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
      TimestampConverter.fromUtc(e.start, account.timezone).toLocalDateTime,
      TimestampConverter.fromUtc(e.end, account.timezone).toLocalDateTime,
      notifications,
      EventStatus(e.status),
      e.userInfo.map(ApiUserInfo(_))
    )
  }
  implicit val userInfoWrites = Json.writes[ApiUserInfo]
  implicit val notificationTimeFormat = Json.format[NotificationTime]
  implicit val eventWrites = Json.writes[ApiEvent]

  /**
    * Notification time api model.
    */
  case class NotificationTime(
    time: LocalDateTime,
    kind: ApiNotificationKind,
    recipient: ApiNotificationRecipient
  ) {

    /**
      * Converts api model to model.
      */
    def toModel(implicit account: User): Event.NotificationTime = Event.NotificationTime(
      TimestampConverter.toUtc(Timestamp.valueOf(time), account.timezone),
      kind.value,
      recipient.value
    )
  }

  object NotificationTime {

    /**
      * Creates api model from model.
      */
    def apply(n: Event.NotificationTime)(implicit account: User): NotificationTime = NotificationTime(
      TimestampConverter.fromUtc(n.time, account.timezone).toLocalDateTime,
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

  /**
    * User related info API model.
    */
  case class ApiUserInfo(
    totalFormsCount: Int,
    answeredFormsCount: Int
  )
  object ApiUserInfo {
    def apply(info: Event.UserInfo): ApiUserInfo = ApiUserInfo(info.totalFormsCount, info.answeredFormsCount)
  }
}
