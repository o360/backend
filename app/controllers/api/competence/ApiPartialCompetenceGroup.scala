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
import io.scalaland.chimney.dsl._
import models.EntityKind
import models.competence.CompetenceGroup
import play.api.libs.json.Json
import utils.RandomGenerator

/**
  * Partial competence group API model.
  */
case class ApiPartialCompetenceGroup(
  name: String,
  description: Option[String],
  machineName: Option[String]
) {
  def toModel(id: Long = 0): CompetenceGroup =
    this
      .into[CompetenceGroup]
      .withFieldConst(_.id, id)
      .withFieldConst(_.kind, EntityKind.Template: EntityKind)
      .withFieldComputed(_.machineName, _.machineName.getOrElse(RandomGenerator.generateMachineName))
      .transform
}

object ApiPartialCompetenceGroup {
  implicit val reads = Json.reads[ApiPartialCompetenceGroup]
}
