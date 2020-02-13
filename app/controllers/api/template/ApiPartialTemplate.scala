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

import controllers.api.notification.{ApiNotificationKind, ApiNotificationRecipient}
import models.template.Template
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import io.scalaland.chimney.dsl._

/**
  * Partial API template model.
  */
case class ApiPartialTemplate(
  name: String,
  subject: String,
  body: String,
  kind: ApiNotificationKind,
  recipient: ApiNotificationRecipient
) {

  def toModel(id: Long = 0): Template =
    this
      .into[Template]
      .withFieldConst(_.id, id)
      .withFieldComputed(_.kind, _.kind.value)
      .withFieldComputed(_.recipient, _.recipient.value)
      .transform
}

object ApiPartialTemplate {

  implicit val templateReads: Reads[ApiPartialTemplate] = (
    (__ \ "name").read[String](maxLength[String](1024)) and
      (__ \ "subject").read[String] and
      (__ \ "body").read[String] and
      (__ \ "kind").read[ApiNotificationKind] and
      (__ \ "recipient").read[ApiNotificationRecipient]
  )(ApiPartialTemplate(_, _, _, _, _))
}
