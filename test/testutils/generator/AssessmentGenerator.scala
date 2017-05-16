package testutils.generator

import models.assessment.Assessment
import models.user.UserShort
import org.scalacheck.Arbitrary

/**
  * Assessment generator for scalacheck.
  */
trait AssessmentGenerator extends UserGenerator {

  implicit val assessmentArb = Arbitrary {
    for {
      user <- Arbitrary.arbitrary[Option[UserShort]]
      formIds <- Arbitrary.arbitrary[Seq[Long]]
    } yield Assessment(user, formIds)
  }
}
