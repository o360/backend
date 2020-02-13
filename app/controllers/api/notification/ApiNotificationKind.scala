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

package controllers.api.notification

import controllers.api.{EnumFormat, EnumFormatHelper}
import models.notification._

/**
  * Kind of notification.
  */
case class ApiNotificationKind(value: NotificationKind) extends EnumFormat[NotificationKind]
object ApiNotificationKind extends EnumFormatHelper[NotificationKind, ApiNotificationKind]("notification kind") {

  override protected def mapping: Map[String, NotificationKind] = Map(
    "preBegin" -> PreBegin,
    "begin" -> Begin,
    "preEnd" -> PreEnd,
    "end" -> End
  )
}
