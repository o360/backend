package testutils.generator

import models.assessment.{Answer, Assessment}
import models.user.UserShort
import org.scalacheck.Arbitrary

/**
  * Assessment generator for scalacheck.
  */
trait AssessmentGenerator extends UserGenerator with AnswerGenerator {

  implicit val assessmentArb = Arbitrary {
    for {
      user <- Arbitrary.arbitrary[Option[UserShort]]
      forms <- Arbitrary.arbitrary[Seq[Answer.Form]]
    } yield Assessment(user, forms)
  }
}
