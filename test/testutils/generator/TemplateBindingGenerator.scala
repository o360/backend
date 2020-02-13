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
import models.notification._
import models.project.TemplateBinding
import org.scalacheck.{Arbitrary, Gen}

/**
  * Template binding generator for scalacheck.
  */
trait TemplateBindingGenerator extends NotificationGenerator {

  implicit val templateBindingArb = Arbitrary {
    for {
      template <- Gen.oneOf(NamedEntity(1, "firstname"), NamedEntity(2, "secondname"), NamedEntity(3, "thirdname"))
      kind <- Arbitrary.arbitrary[NotificationKind]
      recipient <- Arbitrary.arbitrary[NotificationRecipient]
    } yield TemplateBinding(template, kind, recipient)
  }
}
