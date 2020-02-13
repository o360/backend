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

package controllers.api.group

import controllers.api.Response
import models.group.Group
import play.api.libs.json.Json
import io.scalaland.chimney.dsl._

/**
  * Response for group model.
  */
case class ApiGroup(
  id: Long,
  parentId: Option[Long],
  name: String,
  hasChildren: Boolean,
  level: Int
) extends Response

object ApiGroup {
  implicit val writes = Json.writes[ApiGroup]

  def apply(group: Group): ApiGroup = group.transformInto[ApiGroup]
}
