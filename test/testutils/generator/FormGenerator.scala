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

package testutils.generator

import models.form.{Form, FormShort}
import models.form.element._
import org.scalacheck.{Arbitrary, Gen}

/**
  * Form generator for scalacheck.
  */
trait FormGenerator {

  implicit val elementValueArb = Arbitrary {
    for {
      value <- Arbitrary.arbitrary[String]
      weight <- Arbitrary.arbitrary[Option[Double]]
    } yield Form.ElementValue(0, value, weight)
  }

  implicit val elementKindArb = Arbitrary[ElementKind] {
    Gen.oneOf(TextField, TextArea, Checkbox, CheckboxGroup, Radio, Select)
  }

  implicit val elementArb = Arbitrary {
    for {
      kind <- Arbitrary.arbitrary[ElementKind]
      caption <- Arbitrary.arbitrary[String]
      required <- Arbitrary.arbitrary[Boolean]
      values <- Arbitrary.arbitrary[Seq[Form.ElementValue]]
      machineName <- Arbitrary.arbitrary[String]
      hint <- Arbitrary.arbitrary[Option[String]]
    } yield Form.Element(0, kind, caption, required, values, Nil, machineName, hint)
  }

  implicit val formKindArb = Arbitrary[Form.Kind] {
    import Form.Kind._
    Gen.oneOf(Active, Freezed)
  }

  implicit val formShortArb = Arbitrary {
    for {
      name <- Arbitrary.arbitrary[String]
      kind <- Arbitrary.arbitrary[Form.Kind]
      showInAggregation <- Arbitrary.arbitrary[Boolean]
      machineName <- Arbitrary.arbitrary[String]
    } yield FormShort(0, name, kind, showInAggregation, machineName)
  }

  implicit val formArb = Arbitrary {
    for {
      form <- Arbitrary.arbitrary[FormShort]
      elements <- Arbitrary.arbitrary[Seq[Form.Element]]
    } yield form.withElements(elements)
  }
}
