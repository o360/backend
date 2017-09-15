package testutils.generator

import models.NamedEntity
import models.notification._
import models.project.TemplateBinding
import org.scalacheck.{Arbitrary, Gen}

/**
  * Template binding generator for scalacheck.
  */
trait TemplateBindingGenerator extends NotificationGenerator {

  implicit val templateBindingArb = Arbitrary {
    for {
      template <- Gen.oneOf(NamedEntity(1, "firstname"), NamedEntity(2, "secondname"), NamedEntity(3, "thirdname"))
      kind <- Arbitrary.arbitrary[NotificationKind]
      recipient <- Arbitrary.arbitrary[NotificationRecipient]
    } yield TemplateBinding(template, kind, recipient)
  }
}
