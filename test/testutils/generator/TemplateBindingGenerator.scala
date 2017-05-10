package testutils.generator

import models.NamedEntity
import models.notification.Notification
import models.project.TemplateBinding
import org.scalacheck.{Arbitrary, Gen}

/**
  * Template binding generator for scalacheck.
  */
trait TemplateBindingGenerator extends NotificationGenerator {

  implicit val templateBindingArb = Arbitrary {
    for {
      template <- Gen.oneOf(NamedEntity(1, "firstname"), NamedEntity(2, "secondname"), NamedEntity(3, "thirdname"))
      kind <- Arbitrary.arbitrary[Notification.Kind]
      recipient <- Arbitrary.arbitrary[Notification.Recipient]
    } yield TemplateBinding(template, kind, recipient)
  }
}
