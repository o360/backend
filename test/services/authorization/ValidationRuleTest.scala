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

package services.authorization

import org.scalacheck.Arbitrary
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/**
  * Test for validation rule.
  */
class ValidationRuleTest extends PlaySpec with ScalaCheckDrivenPropertyChecks {

  implicit val validationRuleArbitrary = Arbitrary {
    for {
      name <- Arbitrary.arbitrary[String]
      isSuccess <- Arbitrary.arbitrary[Boolean]
    } yield ValidationRule(isSuccess, name)
  }

  "apply" should {
    "create correct validation rule" in {
      forAll { (name: String, precondition: Boolean, check: Boolean) =>
        val rule = ValidationRule(name, precondition)(check)
        rule.name mustBe name
        rule.isSuccess mustBe !precondition || check
      }
    }
  }

  "validate" should {
    "return no errors if there are no errors" in {
      forAll { (rules: Seq[ValidationRule]) =>
        val errors = ValidationRule.validate(rules.map(_.copy(isSuccess = true)))
        errors mustBe empty
      }
    }
    "return error if errors present" in {
      forAll { (rules: Seq[ValidationRule]) =>
        whenever(rules.exists(!_.isSuccess)) {
          val errors = ValidationRule.validate(rules)
          errors mustBe defined
        }
      }
    }
  }
}
