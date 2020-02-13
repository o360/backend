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

package controllers.api.project

import controllers.api.{ApiNamedEntity, Response}
import controllers.api.notification.{ApiNotificationKind, ApiNotificationRecipient}
import models.project.TemplateBinding
import play.api.libs.json.Json

/**
  * API model for email template binding.
  */
case class ApiTemplateBinding(
  template: ApiNamedEntity,
  kind: ApiNotificationKind,
  recipient: ApiNotificationRecipient
) extends Response

object ApiTemplateBinding {
  implicit val writes = Json.writes[ApiTemplateBinding]

  def apply(tb: TemplateBinding): ApiTemplateBinding = ApiTemplateBinding(
    ApiNamedEntity(tb.template),
    ApiNotificationKind(tb.kind),
    ApiNotificationRecipient(tb.recipient)
  )
}
