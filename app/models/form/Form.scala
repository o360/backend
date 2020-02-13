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

package models.form

import models.NamedEntity
import models.form.element.ElementKind

/**
  * Form template model.
  */
case class Form(
  id: Long,
  name: String,
  elements: Seq[Form.Element],
  kind: Form.Kind,
  showInAggregation: Boolean,
  machineName: String
) {

  /**
    * Returns short form.
    */
  def toShort = FormShort(id, name, kind, showInAggregation, machineName)
}

object Form {
  val nameSingular = "form"

  /**
    * Form element.
    */
  case class Element(
    id: Long,
    kind: ElementKind,
    caption: String,
    required: Boolean,
    values: Seq[ElementValue],
    competencies: Seq[ElementCompetence],
    machineName: String,
    hint: Option[String]
  )

  /**
    * Form element value.
    */
  case class ElementValue(
    id: Long,
    caption: String,
    competenceWeight: Option[Double] = None
  )

  /**
    * Form kind.
    */
  sealed trait Kind
  object Kind {

    /**
      * Active form used as template. Active forms can be listed, edited, deleted.
      */
    case object Active extends Kind

    /**
      * Freezed form. Freezed form is a copy of active form assigned to event.
      */
    case object Freezed extends Kind
  }

  /**
    * Element competence.
    */
  case class ElementCompetence(
    competence: NamedEntity,
    factor: Double
  )
}
