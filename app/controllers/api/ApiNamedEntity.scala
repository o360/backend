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

package controllers.api

import models.NamedEntity
import play.api.libs.json.Json

/**
  * API model for named entity.
  */
case class ApiNamedEntity(
  id: Long,
  name: String
)

object ApiNamedEntity {

  def apply(ne: NamedEntity): ApiNamedEntity = ApiNamedEntity(ne.id, ne.name.getOrElse(""))

  implicit val namedEntityFormat = Json.format[ApiNamedEntity]
}
