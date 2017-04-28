package testutils.generator

import java.sql.Timestamp

import models.event.Event
import models.notification.Notification
import org.scalacheck.{Arbitrary, Gen}

/**
  * Event generator for scalacheck.
  */
trait EventGenerator {

  implicit val eventStatusArb = Arbitrary[Event.Status] {
    Gen.oneOf(Event.Status.NotStarted, Event.Status.InProgress, Event.Status.Completed)
  }

  implicit val timestampArb = Arbitrary {
    Gen.choose(0L, 253402300799L).map(new Timestamp(_)) // max year - 9999
  }

  implicit val notificationKindArb = Arbitrary[Notification.Kind] {
    import Notification.Kind._
    Gen.oneOf(PreBegin, Begin, PreEnd, End)
  }

  implicit val notificationRecipientArb = Arbitrary[Notification.Recipient] {
    import Notification.Recipient._
    Gen.oneOf(Respondent, Auditor)
  }

  implicit val notificationTimeArb = Arbitrary {
    for {
      time <- Arbitrary.arbitrary[Timestamp]
      kind <- Arbitrary.arbitrary[Notification.Kind]
      recipient <- Arbitrary.arbitrary[Notification.Recipient]
    } yield Event.NotificationTime(time, kind, recipient)
  }

  implicit val eventArb = Arbitrary {
    for {
      description <- Arbitrary.arbitrary[Option[String]]
      start <- Arbitrary.arbitrary[Timestamp]
      end <- Arbitrary.arbitrary[Timestamp]
      canRevote <- Arbitrary.arbitrary[Boolean]
      notifications <- Arbitrary.arbitrary[Seq[Event.NotificationTime]]

    } yield Event(0, description, start, end, canRevote, notifications)
  }
}
