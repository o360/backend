package testutils.fixture

import java.sql.Timestamp

import com.ninja_squad.dbsetup.Operations.insertInto
import models.event.{Event, EventJob}
import models.notification.Notification

/**
  * Fixture for event job.
  */
trait EventJobFixture extends FixtureHelper with EventFixture {
  _: FixtureSupport =>

  val EventJobs: Seq[EventJob] = Seq(
    EventJob.Upload(1, 1, new Timestamp(123), EventJob.Status.Cancelled),
    EventJob.Upload(2, 2, new Timestamp(456), EventJob.Status.New),
    EventJob.SendNotification(3, 1, Event.NotificationTime(
      new Timestamp(789), Notification.Kind.PreBegin, Notification.Recipient.Respondent), EventJob.Status.Failure)
  )

  addFixtureOperation {
    insertInto("event_job")
      .columns("id", "event_id", "time", "status", "notification_kind", "notification_recipient_kind", "job_type")
      .scalaValues(1, 1, EventJobs(0).time, 3, null, null, 0)
      .scalaValues(2, 2, EventJobs(1).time, 0, null, null, 0)
      .scalaValues(3, 1, EventJobs(2).time, 2, 0, 0, 1)
      .build
  }
}
