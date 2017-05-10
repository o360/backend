package testutils.generator

import models.NamedEntity
import models.project.{Relation, TemplateBinding}
import org.scalacheck.{Arbitrary, Gen}

/**
  * Project relation generator for scalacheck.
  */
trait ProjectRelationGenerator extends TemplateBindingGenerator {

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
      templates <- Arbitrary.arbitrary[Seq[TemplateBinding]]
    } yield Relation(
      0,
      NamedEntity(projectId),
      NamedEntity(groupFrom),
      groupTo.map(NamedEntity(_)),
      NamedEntity(formId),
      kind,
      templates
    )
  }
}
