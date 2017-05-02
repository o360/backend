package testutils.generator

import models.project.Relation
import org.scalacheck.{Arbitrary, Gen}

/**
  * Project relation generator for scalacheck.
  */
trait ProjectRelationGenerator {

  implicit val relationKindArb = Arbitrary[Relation.Kind] {
    Gen.oneOf(Relation.Kind.Classic, Relation.Kind.Survey)
  }

  implicit val relationArb = Arbitrary {
    for {
      projectId <- Arbitrary.arbitrary[Long]
      groupFrom <- Arbitrary.arbitrary[Long]
      groupTo <- Arbitrary.arbitrary[Option[Long]]
      formId <- Arbitrary.arbitrary[Long]
      kind <- Arbitrary.arbitrary[Relation.Kind]
    } yield Relation(0, projectId, groupFrom, groupTo, formId, kind)
  }
}
