package testutils.generator

import models.project.Project
import org.scalacheck.Arbitrary

/**
  * Project generator for scalacheck.
  */
trait ProjectGenerator {

  implicit val projectArb = Arbitrary {
    for {
      name <- Arbitrary.arbitrary[String]
      description <- Arbitrary.arbitrary[Option[String]]
      groupAuditor <- Arbitrary.arbitrary[Long]
    } yield Project(0, name, description, groupAuditor)
  }
}
