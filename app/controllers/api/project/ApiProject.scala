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

package controllers.api.project

import controllers.api.{ApiNamedEntity, Response}
import models.project.Project
import models.user.User
import play.api.libs.json.Json
import io.scalaland.chimney.dsl._

/**
  * Project API model.
  */
case class ApiProject(
  id: Long,
  name: String,
  description: Option[String],
  groupAuditor: Option[ApiNamedEntity],
  templates: Option[Seq[ApiTemplateBinding]],
  formsOnSamePage: Boolean,
  canRevote: Boolean,
  isAnonymous: Boolean,
  hasInProgressEvents: Boolean,
  machineName: String
) extends Response

object ApiProject {

  implicit val projectWrites = Json.writes[ApiProject]

  def apply(project: Project)(implicit account: User): ApiProject = {
    val (groupAuditor, templates) = account.role match {
      case User.Role.Admin =>
        (Some(ApiNamedEntity(project.groupAuditor)), Some(project.templates.map(ApiTemplateBinding(_))))
      case User.Role.User =>
        (None, None)
    }

    project
      .into[ApiProject]
      .withFieldConst(_.groupAuditor, groupAuditor)
      .withFieldConst(_.templates, templates)
      .transform
  }
}
