package testutils.generator

import models.project.Project
import org.scalacheck.Arbitrary

/**
  * Project generator for scalacheck.
  */
trait ProjectGenerator {

  implicit val relationArb = Arbitrary {
    for {
      groupFrom <- Arbitrary.arbitrary[Long]
      groupTo <- Arbitrary.arbitrary[Long]
      formId <- Arbitrary.arbitrary[Long]
    } yield Project.Relation(groupFrom, groupTo, formId)
  }

  implicit val projectArb = Arbitrary {
    for {
      name <- Arbitrary.arbitrary[String]
      description <- Arbitrary.arbitrary[Option[String]]
      groupAuditor <- Arbitrary.arbitrary[Long]
      relations <- Arbitrary.arbitrary[Seq[Project.Relation]]
    } yield Project(0, name, description, groupAuditor, relations)
  }
}
