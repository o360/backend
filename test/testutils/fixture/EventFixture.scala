package testutils.fixture

import java.sql.Timestamp
import java.time.LocalDateTime

import com.ninja_squad.dbsetup.Operations._
import models.event.Event
import models.notification._

/**
  * Event model fixture.
  */
trait EventFixture extends FixtureHelper { self: FixtureSupport =>

  val Events = EventFixture.values

  addFixtureOperation {
    sequenceOf(
      insertInto("event")
        .columns("id", "start_time", "end_time", "description", "is_preparing")
        .scalaValues(1, Events(0).start, Events(0).end, "description", false)
        .scalaValues(2, Events(1).start, Events(1).end, "completed", false)
        .scalaValues(3, Events(2).start, Events(2).end, "notStarted", true)
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

object EventFixture {
  val values = Seq(
    Event(
      1,
      Some("description"),
      Timestamp.valueOf(LocalDateTime.of(2017, 1, 2, 12, 30)),
      Timestamp.valueOf(LocalDateTime.of(2017, 1, 5, 12, 30)),
      Seq(
        Event.NotificationTime(
          Timestamp.valueOf(LocalDateTime.of(2017, 1, 2, 12, 0)),
          PreBegin,
          Respondent
        ),
        Event.NotificationTime(
          Timestamp.valueOf(LocalDateTime.of(2017, 1, 2, 12, 30)),
          Begin,
          Respondent
        ),
        Event.NotificationTime(
          Timestamp.valueOf(LocalDateTime.of(2017, 1, 5, 12, 30)),
          End,
          Auditor
        )
      )
    ),
    Event(
      2,
      Some("completed"),
      new Timestamp(0),
      new Timestamp(500),
      Nil
    ),
    Event(
      3,
      Some("notStarted"),
      new Timestamp(Long.MaxValue - 500),
      new Timestamp(Long.MaxValue),
      Nil,
      isPreparing = true
    ),
    Event(
      4,
      Some("inProgress"),
      new Timestamp(0),
      new Timestamp(Long.MaxValue),
      Nil
    )
  )
}
