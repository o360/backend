package testutils.generator

import models.notification._
import models.template.Template
import org.scalacheck.Arbitrary

/**
  * Template generator for scalacheck.
  */
trait TemplateGenerator extends NotificationGenerator {

  implicit val templateArb = Arbitrary {
    for {
      name <- Arbitrary.arbitrary[String]
      subject <- Arbitrary.arbitrary[String]
      body <- Arbitrary.arbitrary[String]
      kind <- Arbitrary.arbitrary[NotificationKind]
      recipient <- Arbitrary.arbitrary[NotificationRecipient]
    } yield Template(0, name, subject, body, kind, recipient)
  }
}
