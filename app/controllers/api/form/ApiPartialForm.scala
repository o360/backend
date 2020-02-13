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

package controllers.api.form

import controllers.api.Response
import controllers.api.form.ApiForm.ApiElementKind
import models.form.Form
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.RandomGenerator
import io.scalaland.chimney.dsl._
import models.NamedEntity

/**
  * Api partial form model.
  */
case class ApiPartialForm(
  name: String,
  elements: Option[Seq[ApiPartialForm.Element]],
  showInAggregation: Boolean,
  machineName: Option[String]
) {

  def toModel(id: Long = 0): Form =
    this
      .into[Form]
      .withFieldConst(_.id, id)
      .withFieldComputed(_.elements, _.elements.getOrElse(Seq.empty[ApiPartialForm.Element]).map(_.toModel))
      .withFieldConst(_.kind, Form.Kind.Active: Form.Kind)
      .withFieldComputed(_.machineName, _.machineName.getOrElse(RandomGenerator.generateMachineName))
      .transform
}

object ApiPartialForm {

  implicit val elementCompetenceReads = Json.reads[ElementCompetence]

  implicit val elementValueReads: Reads[ElementValue] = (
    (__ \ "caption").read[String](maxLength[String](1024)) and
      (__ \ "competenceWeight").readNullable[Double]
  )(ElementValue)

  implicit val elementReads: Reads[Element] = (
    (__ \ "kind").read[ApiElementKind] and
      (__ \ "caption").read[String](maxLength[String](1024)) and
      (__ \ "required").read[Boolean] and
      (__ \ "values").readNullable[Seq[ElementValue]] and
      (__ \ "competencies").readNullable[Seq[ElementCompetence]] and
      (__ \ "machineName").readNullable[String] and
      (__ \ "hint").readNullable[String]
  )(Element)

  implicit val formReads: Reads[ApiPartialForm] = (
    (__ \ "name").read[String](maxLength[String](1024)) and
      (__ \ "elements").readNullable[Seq[ApiPartialForm.Element]] and
      (__ \ "showInAggregation").read[Boolean] and
      (__ \ "machineName").readNullable[String]
  )(ApiPartialForm.apply _)

  /**
    * Form element api model.
    */
  case class Element(
    kind: ApiElementKind,
    caption: String,
    required: Boolean,
    values: Option[Seq[ElementValue]],
    competencies: Option[Seq[ElementCompetence]],
    machineName: Option[String],
    hint: Option[String]
  ) extends Response {

    def toModel: Form.Element =
      this
        .into[Form.Element]
        .withFieldConst(_.id, 0L)
        .withFieldComputed(_.kind, _.kind.value)
        .withFieldComputed(_.values, _.values.getOrElse(Seq.empty[ApiPartialForm.ElementValue]).map(_.toModel))
        .withFieldComputed(_.competencies, _.competencies.toSeq.flatMap(_.map(_.toModel)))
        .withFieldComputed(_.machineName, _.machineName.getOrElse(RandomGenerator.generateMachineName))
        .transform
  }

  /**
    * Form element value api model.
    */
  case class ElementValue(
    caption: String,
    competenceWeight: Option[Double]
  ) extends Response {

    def toModel = Form.ElementValue(
      0,
      caption,
      competenceWeight
    )
  }

  /**
    * Form element competence API model.
    */
  case class ElementCompetence(
    competenceId: Long,
    factor: Double
  ) {
    def toModel = Form.ElementCompetence(
      NamedEntity(competenceId),
      factor
    )
  }
}
