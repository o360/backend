package services.authorization

import org.scalacheck.Arbitrary
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec

/**
  * Test for validation rule.
  */
class ValidationRuleTest extends PlaySpec with GeneratorDrivenPropertyChecks {

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
