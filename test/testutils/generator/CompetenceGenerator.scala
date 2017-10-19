package testutils.generator

import models.EntityKind
import models.competence.Competence
import org.scalacheck.{Arbitrary, Gen}
import testutils.fixture.CompetenceGroupFixture

/**
  * Competence generator for scalacheck.
  */
trait CompetenceGenerator extends TemplateBindingGenerator {

  implicit val competenceArb = Arbitrary {
    for {
      groupId <- Gen.oneOf(CompetenceGroupFixture.values.map(_.id))
      name <- Arbitrary.arbitrary[String]
      description <- Arbitrary.arbitrary[Option[String]]
      kind <- Gen.oneOf(EntityKind.Template, EntityKind.Freezed)
      machineName <- Arbitrary.arbitrary[String]
    } yield Competence(0, groupId, name, description, kind, machineName)
  }
}
