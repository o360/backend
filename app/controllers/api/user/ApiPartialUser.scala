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

package controllers.api.user

import java.time.ZoneId

import models.user.User
import play.api.libs.json.Json

/**
  * Partial user model for self-updates.
  */
case class ApiPartialUser(
  name: Option[String],
  email: Option[String],
  gender: Option[ApiUser.ApiGender],
  timezone: ZoneId,
  termsApproved: Boolean
) {
  def applyTo(user: User): User = {
    user.copy(
      name = name,
      email = email,
      gender = gender.map(_.value),
      timezone = timezone,
      termsApproved = termsApproved
    )
  }
}

object ApiPartialUser {
  implicit val reads = Json.reads[ApiPartialUser]
}
