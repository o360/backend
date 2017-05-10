package testutils.fixture

import java.sql.Timestamp
import java.time.LocalDateTime

import com.ninja_squad.dbsetup.Operations._
import models.event.Event
import models.notification.Notification

/**
  * Event model fixture.
  */
trait EventFixture extends FixtureHelper {
  self: FixtureSupport =>

  val Events = Seq(
    Event(
      1,
      Some("description"),
      Timestamp.valueOf(LocalDateTime.of(2017, 1, 2, 12, 30)),
      Timestamp.valueOf(LocalDateTime.of(2017, 1, 5, 12, 30)),
      canRevote = true,
      Seq(
        Event.NotificationTime(
          Timestamp.valueOf(LocalDateTime.of(2017, 1, 2, 12, 0)),
          Notification.Kind.PreBegin,
          Notification.Recipient.Respondent
        ),
        Event.NotificationTime(
          Timestamp.valueOf(LocalDateTime.of(2017, 1, 2, 12, 30)),
          Notification.Kind.Begin,
          Notification.Recipient.Respondent
        ),
        Event.NotificationTime(
          Timestamp.valueOf(LocalDateTime.of(2017, 1, 5, 12, 30)),
          Notification.Kind.End,
          Notification.Recipient.Auditor
        )
      )
    ),
    Event(
      2,
      Some("completed"),
      new Timestamp(0),
      new Timestamp(500),
      canRevote = false,
      Nil
    ),
    Event(
      3,
      Some("notStarted"),
      new Timestamp(Long.MaxValue - 500),
      new Timestamp(Long.MaxValue),
      canRevote = false,
      Nil
    ),
    Event(
      4,
      Some("inProgress"),
      new Timestamp(0),
      new Timestamp(Long.MaxValue),
      canRevote = false,
      Nil
    )
  )

  addFixtureOperation {
    sequenceOf(
      insertInto("event")
        .columns("id", "start_time", "end_time", "description", "can_revote")
        .scalaValues(1, Events(0).start, Events(0).end, "description", true)
        .scalaValues(2, Events(1).start, Events(1).end, "completed", false)
        .scalaValues(3, Events(2).start, Events(2).end, "notStarted", false)
        .scalaValues(4, Events(3).start, Events(3).end, "inProgress", false)
        .build,
      insertInto("event_notification")
        .columns("id", "event_id", "time", "kind", "recipient_kind")
        .scalaValues(1, 1, Events(0).notifications(0).time, 0, 0)
        .scalaValues(2, 1, Events(0).notifications(1).time, 1, 0)
        .scalaValues(3, 1, Events(0).notifications(2).time, 3, 1)
        .build
    )
  }
}
