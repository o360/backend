package testutils.generator

import models.project.Project
import org.scalacheck.{Arbitrary, Gen}

/**
  * Project generator for scalacheck.
  */
trait ProjectGenerator {

  implicit val relationKindArb = Arbitrary[Project.RelationKind] {
    Gen.oneOf(Project.RelationKind.Classic, Project.RelationKind.Survey)
  }

  implicit val relationArb = Arbitrary {
    for {
      groupFrom <- Arbitrary.arbitrary[Long]
      groupTo <- Arbitrary.arbitrary[Option[Long]]
      formId <- Arbitrary.arbitrary[Long]
      kind <- Arbitrary.arbitrary[Project.RelationKind]
    } yield Project.Relation(groupFrom, groupTo, formId, kind)
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
