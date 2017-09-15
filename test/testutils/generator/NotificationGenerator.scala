package testutils.generator

import models.notification._
import org.scalacheck.{Arbitrary, Gen}

/**
  * Notification generator for scalacheck.
  */
trait NotificationGenerator {

  implicit val notificationKindArb = Arbitrary[NotificationKind] {
    Gen.oneOf(PreBegin, Begin, PreEnd, End)
  }

  implicit val notificationRecipientArb = Arbitrary[NotificationRecipient] {
    Gen.oneOf(Respondent, Auditor)
  }
}
