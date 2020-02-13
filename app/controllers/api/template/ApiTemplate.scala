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

package controllers.api.template

import controllers.api.Response
import controllers.api.notification.{ApiNotificationKind, ApiNotificationRecipient}
import models.template.Template
import play.api.libs.json.Json
import io.scalaland.chimney.dsl._

/**
  * API template model.
  */
case class ApiTemplate(
  id: Long,
  name: String,
  subject: String,
  body: String,
  kind: ApiNotificationKind,
  recipient: ApiNotificationRecipient
) extends Response

object ApiTemplate {
  def apply(t: Template): ApiTemplate =
    t.into[ApiTemplate]
      .withFieldComputed(_.kind, x => ApiNotificationKind(x.kind))
      .withFieldComputed(_.recipient, x => ApiNotificationRecipient(x.recipient))
      .transform

  implicit val templateWrites = Json.writes[ApiTemplate]
}
