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

package controllers.api.event

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
  userInfo: Option[ApiEvent.ApiUserInfo],
  isPreparing: Boolean
) extends Response

object ApiEvent {

  /**
    * Creates api model form model.
    */
  def apply(e: Event)(implicit account: User): ApiEvent = {
    val notifications = account.role match {
      case User.Role.Admin => Some(e.notifications.map(NotificationTime(_)))
      case User.Role.User  => None
    }

    ApiEvent(
      e.id,
      e.description,
      TimestampConverter.fromUtc(e.start, account.timezone),
      TimestampConverter.fromUtc(e.end, account.timezone),
      notifications,
      EventStatus(e.status),
      e.userInfo.map(ApiUserInfo(_)),
      e.isPreparing
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
      TimestampConverter.toUtc(time, account.timezone),
      kind.value,
      recipient.value
    )
  }

  object NotificationTime {

    /**
      * Creates api model from model.
      */
    def apply(n: Event.NotificationTime)(implicit account: User): NotificationTime = NotificationTime(
      TimestampConverter.fromUtc(n.time, account.timezone),
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
