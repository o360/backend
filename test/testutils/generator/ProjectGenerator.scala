package testutils.generator

import models.NamedEntity
import models.project.{Project, TemplateBinding}
import org.scalacheck.Arbitrary

/**
  * Project generator for scalacheck.
  */
trait ProjectGenerator extends TemplateBindingGenerator {

  implicit val projectArb = Arbitrary {
    for {
      name <- Arbitrary.arbitrary[String]
      description <- Arbitrary.arbitrary[Option[String]]
      groupAuditor <- Arbitrary.arbitrary[Long]
      templates <- Arbitrary.arbitrary[Seq[TemplateBinding]]
    } yield Project(0, name, description, NamedEntity(groupAuditor), templates)
  }
}
