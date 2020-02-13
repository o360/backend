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

import java.time.LocalDateTime

import controllers.api.ApiNamedEntity
import models.invite.Invite
import models.user.User
import play.api.libs.json.Json
import utils.TimestampConverter

/**
  * API invite model.
  */
case class ApiInvite(
  code: String,
  email: String,
  groups: Set[ApiNamedEntity],
  creationTime: LocalDateTime,
  activationTime: Option[LocalDateTime]
)

object ApiInvite {
  def apply(invite: Invite)(implicit account: User): ApiInvite = ApiInvite(
    invite.code,
    invite.email,
    invite.groups.map(ApiNamedEntity(_)),
    TimestampConverter.fromUtc(invite.creationTime, account.timezone),
    invite.activationTime.map(TimestampConverter.fromUtc(_, account.timezone))
  )

  implicit val writes = Json.writes[ApiInvite]
}
