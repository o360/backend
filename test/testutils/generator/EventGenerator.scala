package testutils.generator

import java.time.LocalDateTime

import models.event.Event
import models.notification._
import org.scalacheck.{Arbitrary, Gen}

/**
  * Event generator for scalacheck.
  */
trait EventGenerator extends NotificationGenerator with TimeGenerator {

  implicit val eventStatusArb = Arbitrary[Event.Status] {
    Gen.oneOf(Event.Status.NotStarted, Event.Status.InProgress, Event.Status.Completed)
  }

  implicit val notificationTimeArb = Arbitrary {
    for {
      time <- Arbitrary.arbitrary[LocalDateTime]
      kind <- Arbitrary.arbitrary[NotificationKind]
      recipient <- Arbitrary.arbitrary[NotificationRecipient]
    } yield Event.NotificationTime(time, kind, recipient)
  }

  implicit val eventArb = Arbitrary {
    for {
      id <- Arbitrary.arbitrary[Long]
      description <- Arbitrary.arbitrary[Option[String]]
      start <- Arbitrary.arbitrary[LocalDateTime]
      end <- Arbitrary.arbitrary[LocalDateTime]
      notifications <- Arbitrary.arbitrary[Seq[Event.NotificationTime]]
      isPreparing <- Arbitrary.arbitrary[Boolean]
    } yield Event(id, description, start, end, notifications, isPreparing = isPreparing)
  }
}
