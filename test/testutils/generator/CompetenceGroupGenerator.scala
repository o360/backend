package testutils.generator

import models.EntityKind
import models.competence.CompetenceGroup
import org.scalacheck.{Arbitrary, Gen}

/**
  * Competence group generator for scalacheck.
  */
trait CompetenceGroupGenerator extends TemplateBindingGenerator {

  implicit val competenceGroupArb = Arbitrary {
    for {
      name <- Arbitrary.arbitrary[String]
      description <- Arbitrary.arbitrary[Option[String]]
      kind <- Gen.oneOf(EntityKind.Template, EntityKind.Freezed)
      machineName <- Arbitrary.arbitrary[String]
    } yield CompetenceGroup(0, name, description, kind, machineName)
  }
}
