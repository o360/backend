package testutils.generator

import java.sql.Timestamp

import models.event.{Event, EventJob}
import models.notification.Notification
import org.scalacheck.{Arbitrary, Gen}

/**
  * Scalacheck generator for event job.
  */
trait EventJobGenerator extends NotificationGenerator {

  implicit val eventStatusArb = Arbitrary[EventJob.Status] {
    import EventJob.Status._
    Gen.oneOf(New, Success, Failure, Cancelled)
  }

  implicit val eventJobArb = Arbitrary[EventJob] {
    val uploadJobGen = for {
      eventId <- Arbitrary.arbitrary[Long]
      time <- Gen.choose(1L, 10000L)
      status <- Arbitrary.arbitrary[EventJob.Status]
    } yield EventJob.Upload(0, eventId, new Timestamp(time), status)

    val sendNotificationJobGen = for {
      eventId <- Arbitrary.arbitrary[Long]
      time <- Gen.choose(1L, 10000L)
      status <- Arbitrary.arbitrary[EventJob.Status]
      kind <- Arbitrary.arbitrary[Notification.Kind]
      recipient <- Arbitrary.arbitrary[Notification.Recipient]
    } yield EventJob.SendNotification(0, eventId, Event.NotificationTime(new Timestamp(time), kind, recipient), status)

    Gen.oneOf(uploadJobGen, sendNotificationJobGen)
  }

}
