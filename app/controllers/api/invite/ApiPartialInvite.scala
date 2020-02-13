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

package controllers.api.invite

import models.NamedEntity
import models.invite.Invite
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
  * API invite model.
  */
case class ApiPartialInvite(
  email: String,
  groups: Set[Long]
) {
  def toModel = Invite(email, groups.map(NamedEntity(_)))
}

object ApiPartialInvite {
  implicit val reads: Reads[ApiPartialInvite] = (
    (__ \ "email").read[String](maxLength[String](255)) and
      (__ \ "groups").read[Set[Long]]
  )(ApiPartialInvite(_, _))
}
