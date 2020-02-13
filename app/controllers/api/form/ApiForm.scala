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

import controllers.api.{ApiNamedEntity, EnumFormat, EnumFormatHelper, Response}
import models.form.{Form, FormShort}
import models.form.element._
import play.api.libs.json._
import io.scalaland.chimney.dsl._

/**
  * Api form model.
  */
case class ApiForm(
  id: Long,
  name: String,
  elements: Seq[ApiForm.Element],
  showInAggregation: Boolean,
  machineName: String
) extends Response

object ApiForm {

  /**
    * Converts form to ApiForm.
    *
    * @param form form
    */
  def apply(form: Form, includeCompetencies: Boolean = false): ApiForm =
    form
      .into[ApiForm]
      .withFieldComputed(_.elements, _.elements.map(Element(_, includeCompetencies)))
      .transform

  /**
    * Converts short form to ApiForm.
    *
    * @param form short form
    */
  def apply(form: FormShort): ApiForm =
    form
      .into[ApiForm]
      .withFieldConst(_.elements, Seq.empty[ApiForm.Element])
      .transform

  implicit val competenceWrites = Json.writes[ElementCompetence]
  implicit val elementValueWrites = Json.writes[ElementValue]
  implicit val elementWrites = Json.writes[Element]
  implicit val formWrites = Json.writes[ApiForm]

  /**
    * Form element api model.
    */
  case class Element(
    id: Long,
    kind: ApiElementKind,
    caption: String,
    required: Boolean,
    values: Option[Seq[ElementValue]],
    competencies: Option[Seq[ElementCompetence]],
    machineName: String,
    hint: Option[String]
  ) extends Response

  object Element {

    /**
      * Converts form element to api form element.
      */
    def apply(element: Form.Element, includeCompetencies: Boolean = false): Element = {
      val competencies = if (includeCompetencies) {
        Some(element.competencies.map(ElementCompetence.fromModel))
      } else None

      val values = element.values.map(_.transformInto[ElementValue])
      element
        .into[Element]
        .withFieldComputed(_.kind, x => ApiElementKind(x.kind))
        .withFieldConst(_.values, if (values.isEmpty) None else Some(values))
        .withFieldConst(_.competencies, competencies)
        .transform
    }
  }

  /**
    * Element kind api model.
    */
  case class ApiElementKind(value: ElementKind) extends EnumFormat[ElementKind]
  object ApiElementKind extends EnumFormatHelper[ElementKind, ApiElementKind]("element kind") {

    override protected def mapping: Map[String, ElementKind] = Map(
      "textfield" -> TextField,
      "textarea" -> TextArea,
      "checkbox" -> Checkbox,
      "checkboxgroup" -> CheckboxGroup,
      "radio" -> Radio,
      "select" -> Select,
      "likedislike" -> LikeDislike
    )
  }

  /**
    * Form element value api model.
    */
  case class ElementValue(
    id: Long,
    caption: String,
    competenceWeight: Option[Double]
  ) extends Response

  /**
    * Form element competence API model.
    */
  case class ElementCompetence(
    competence: ApiNamedEntity,
    factor: Double
  ) extends Response

  object ElementCompetence {
    def fromModel(ec: Form.ElementCompetence) = ElementCompetence(
      ApiNamedEntity(ec.competence),
      ec.factor
    )
  }
}
