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
import models.project.{Project, TemplateBinding}
import org.scalacheck.Arbitrary

/**
  * Project generator for scalacheck.
  */
trait ProjectGenerator extends TemplateBindingGenerator {

  implicit val projectArb = Arbitrary {
    for {
      name <- Arbitrary.arbitrary[String]
      description <- Arbitrary.arbitrary[Option[String]]
      groupAuditor <- Arbitrary.arbitrary[Long]
      templates <- Arbitrary.arbitrary[Seq[TemplateBinding]]
      formsOnSamePage <- Arbitrary.arbitrary[Boolean]
      canRevote <- Arbitrary.arbitrary[Boolean]
      isAnonymous <- Arbitrary.arbitrary[Boolean]
      machineName <- Arbitrary.arbitrary[String]
    } yield Project(
      0,
      name,
      description,
      NamedEntity(groupAuditor),
      templates,
      formsOnSamePage,
      canRevote,
      isAnonymous,
      hasInProgressEvents = false,
      machineName
    )
  }
}
