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

package controllers.api.competence

import controllers.api.Response
import models.competence.Competence
import play.api.libs.json.Json
import io.scalaland.chimney.dsl._

/**
  * Competence API model.
  */
case class ApiCompetence(
  id: Long,
  groupId: Long,
  name: String,
  description: Option[String],
  machineName: String
) extends Response

object ApiCompetence {
  implicit val writes = Json.writes[ApiCompetence]

  def fromModel(c: Competence) = c.transformInto[ApiCompetence]
}
