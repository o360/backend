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

import controllers.api.Response
import models.user.UserShort
import play.api.libs.json.Json
import io.scalaland.chimney.dsl._

/**
  * Short user API model.
  */
case class ApiShortUser(
  id: Long,
  firstName: String,
  lastName: String,
  gender: ApiUser.ApiGender,
  hasPicture: Boolean
) extends Response

object ApiShortUser {

  implicit val shortUserWrites = Json.writes[ApiShortUser]

  def apply(user: UserShort): ApiShortUser =
    user
      .into[ApiShortUser]
      .withFieldComputed(_.gender, x => ApiUser.ApiGender(x.gender))
      .transform
}
