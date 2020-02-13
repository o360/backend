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

import models.NamedEntity
import models.project.{Relation, TemplateBinding}
import org.scalacheck.{Arbitrary, Gen}

/**
  * Project relation generator for scalacheck.
  */
trait ProjectRelationGenerator extends TemplateBindingGenerator {

  implicit val relationKindArb = Arbitrary[Relation.Kind] {
    Gen.oneOf(Relation.Kind.Classic, Relation.Kind.Survey)
  }

  implicit val relationArb = Arbitrary {
    for {
      projectId <- Arbitrary.arbitrary[Long]
      groupFrom <- Arbitrary.arbitrary[Long]
      groupTo <- Arbitrary.arbitrary[Option[Long]]
      formId <- Arbitrary.arbitrary[Long]
      kind <- Arbitrary.arbitrary[Relation.Kind]
      templates <- Arbitrary.arbitrary[Seq[TemplateBinding]]
      canSelfVote <- Arbitrary.arbitrary[Boolean]
      canSkip <- Arbitrary.arbitrary[Boolean]
    } yield Relation(
      0,
      NamedEntity(projectId),
      NamedEntity(groupFrom),
      groupTo.map(NamedEntity(_)),
      NamedEntity(formId),
      kind,
      templates,
      hasInProgressEvents = false,
      canSelfVote,
      canSkip
    )
  }
}
