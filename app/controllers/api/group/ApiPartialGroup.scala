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

import models.group.Group
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
  * Request for group creating and updating.
  */
case class ApiPartialGroup(
  parentId: Option[Long],
  name: String
) {
  def toModel(id: Long) = Group(
    id,
    parentId,
    name,
    hasChildren = false,
    level = 0
  )
}

object ApiPartialGroup {
  implicit val reads: Reads[ApiPartialGroup] = (
    (__ \ "parentId").readNullable[Long] and
      (__ \ "name").read[String](maxLength[String](1024))
  )(ApiPartialGroup(_, _))
}
