package testutils.generator

import models.notification.Notification
import org.scalacheck.{Arbitrary, Gen}

/**
  * Notification generator for scalacheck.
  */
trait NotificationGenerator {

  implicit val notificationKindArb = Arbitrary[Notification.Kind] {
    import Notification.Kind._
    Gen.oneOf(PreBegin, Begin, PreEnd, End)
  }

  implicit val notificationRecipientArb = Arbitrary[Notification.Recipient] {
    import Notification.Recipient._
    Gen.oneOf(Respondent, Auditor)
  }
}
