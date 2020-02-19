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

package controllers.api.auth

import play.api.libs.json.{Json, Writes}
import controllers.api.Response

/**
  * Api model for authentication token.
  */
case class ApiToken(
  token: String
) extends Response

object ApiToken {
  implicit val writes: Writes[ApiToken] = Json.writes[ApiToken]
}
