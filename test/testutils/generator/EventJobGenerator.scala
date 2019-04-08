package testutils.generator

import java.time.LocalDateTime

import models.event.{Event, EventJob}
import models.notification._
import org.scalacheck.{Arbitrary, Gen}

/**
  * Scalacheck generator for event job.
  */
trait EventJobGenerator extends NotificationGenerator with TimeGenerator {

  implicit val eventStatusArb = Arbitrary[EventJob.Status] {
    import EventJob.Status._
    Gen.oneOf(New, Success, Failure, Cancelled)
  }

  implicit val eventJobArb = Arbitrary[EventJob] {
    val uploadJobGen = for {
      eventId <- Arbitrary.arbitrary[Long]
      time <- Arbitrary.arbitrary[LocalDateTime]
      status <- Arbitrary.arbitrary[EventJob.Status]
    } yield EventJob.Upload(0, eventId, time, status)

    val sendNotificationJobGen = for {
      eventId <- Arbitrary.arbitrary[Long]
      time <- Arbitrary.arbitrary[LocalDateTime]
      status <- Arbitrary.arbitrary[EventJob.Status]
      kind <- Arbitrary.arbitrary[NotificationKind]
      recipient <- Arbitrary.arbitrary[NotificationRecipient]
    } yield EventJob.SendNotification(0, eventId, Event.NotificationTime(time, kind, recipient), status)

    Gen.oneOf(uploadJobGen, sendNotificationJobGen)
  }

}
