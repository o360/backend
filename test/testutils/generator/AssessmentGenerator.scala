package testutils.generator

import models.assessment.{Answer, Assessment}
import models.user.User
import org.scalacheck.Arbitrary

/**
  * Assessment generator for scalacheck.
  */
trait AssessmentGenerator extends UserGenerator with AnswerGenerator {

  implicit val assessmentArb = Arbitrary {
    for {
      user <- Arbitrary.arbitrary[Option[User]]
      forms <- Arbitrary.arbitrary[Seq[Answer]]
    } yield Assessment(user, forms)
  }
}
