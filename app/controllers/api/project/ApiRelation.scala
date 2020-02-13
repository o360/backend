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

import controllers.api.{ApiNamedEntity, EnumFormat, EnumFormatHelper, Response}
import models.project.Relation
import play.api.libs.json.Json

/**
  * Relation API model.
  */
case class ApiRelation(
  id: Long,
  project: ApiNamedEntity,
  groupFrom: ApiNamedEntity,
  groupTo: Option[ApiNamedEntity],
  form: ApiNamedEntity,
  kind: ApiRelation.Kind,
  templates: Seq[ApiTemplateBinding],
  hasInProgressEvents: Boolean,
  canSelfVote: Boolean,
  canSkipAnswers: Boolean
) extends Response

object ApiRelation {
  def apply(r: Relation): ApiRelation = ApiRelation(
    r.id,
    ApiNamedEntity(r.project),
    ApiNamedEntity(r.groupFrom),
    r.groupTo.map(ApiNamedEntity(_)),
    ApiNamedEntity(r.form),
    Kind(r.kind),
    r.templates.map(ApiTemplateBinding(_)),
    r.hasInProgressEvents,
    r.canSelfVote,
    r.canSkipAnswers
  )

  case class Kind(value: Relation.Kind) extends EnumFormat[Relation.Kind]
  object Kind extends EnumFormatHelper[Relation.Kind, Kind]("relation kind") {

    override protected def mapping: Map[String, Relation.Kind] = Map(
      "classic" -> Relation.Kind.Classic,
      "survey" -> Relation.Kind.Survey
    )
  }

  implicit val relationWrites = Json.writes[ApiRelation]
}
