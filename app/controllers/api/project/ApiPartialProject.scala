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

import models.NamedEntity
import models.project.Project
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.RandomGenerator
import io.scalaland.chimney.dsl._

/**
  * Project partial API model.
  */
case class ApiPartialProject(
  name: String,
  description: Option[String],
  groupAuditorId: Long,
  templates: Seq[ApiPartialTemplateBinding],
  formsOnSamePage: Boolean,
  canRevote: Boolean,
  isAnonymous: Boolean,
  machineName: Option[String]
) {

  def toModel(id: Long = 0): Project =
    this
      .into[Project]
      .withFieldConst(_.id, id)
      .withFieldComputed(_.groupAuditor, x => NamedEntity(x.groupAuditorId))
      .withFieldComputed(_.templates, _.templates.map(_.toModel))
      .withFieldConst(_.hasInProgressEvents, false)
      .withFieldComputed(_.machineName, _.machineName.getOrElse(RandomGenerator.generateMachineName))
      .transform
}

object ApiPartialProject {

  implicit val reads: Reads[ApiPartialProject] = (
    (__ \ "name").read[String](maxLength[String](1024)) and
      (__ \ "description").readNullable[String](maxLength[String](1024)) and
      (__ \ "groupAuditorId").read[Long] and
      (__ \ "templates").read[Seq[ApiPartialTemplateBinding]] and
      (__ \ "formsOnSamePage").read[Boolean] and
      (__ \ "canRevote").read[Boolean] and
      (__ \ "isAnonymous").read[Boolean] and
      (__ \ "machineName").readNullable[String]
  )(ApiPartialProject(_, _, _, _, _, _, _, _))
}
