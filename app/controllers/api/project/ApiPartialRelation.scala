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
import models.project.Relation
import play.api.libs.json.Json

/**
  * Relation partial API model.
  */
case class ApiPartialRelation(
  projectId: Long,
  groupFromId: Long,
  groupToId: Option[Long],
  formId: Long,
  kind: ApiRelation.Kind,
  templates: Seq[ApiPartialTemplateBinding],
  canSelfVote: Boolean,
  canSkip: Boolean
) {

  def toModel(id: Long = 0) = Relation(
    id,
    NamedEntity(projectId),
    NamedEntity(groupFromId),
    groupToId.map(NamedEntity(_)),
    NamedEntity(formId),
    kind.value,
    templates.map(_.toModel),
    hasInProgressEvents = false,
    canSelfVote,
    canSkip
  )
}

object ApiPartialRelation {
  implicit val relationReads = Json.reads[ApiPartialRelation]
}
