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

package models.event

import java.time.{LocalDateTime, ZoneId}

import models.NamedEntity
import models.notification._
import utils.TimestampConverter

/**
  * Event model.
  */
case class Event(
  id: Long,
  description: Option[String],
  start: LocalDateTime,
  end: LocalDateTime,
  notifications: Seq[Event.NotificationTime],
  userInfo: Option[Event.UserInfo] = None,
  isPreparing: Boolean = false
) {
  private val currentTime = TimestampConverter.now

  /**
    * Status of event.
    */
  val status: Event.Status =
    if (currentTime.isBefore(start) || isPreparing) Event.Status.NotStarted
    else if (currentTime.isBefore(end)) Event.Status.InProgress
    else Event.Status.Completed

  /**
    * Event text representation.
    */
  def caption(zone: ZoneId): String =
    s"Event ${description.map(_ + " ").getOrElse("")}" +
      s"(${TimestampConverter.toPrettyString(start, zone)} - ${TimestampConverter.toPrettyString(end, zone)})"

  def toNamedEntity(zone: ZoneId) = NamedEntity(id, caption(zone))
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
    time: LocalDateTime,
    kind: NotificationKind,
    recipient: NotificationRecipient
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

  /**
    * User related info.
    */
  case class UserInfo(
    totalFormsCount: Int,
    answeredFormsCount: Int
  )
}
