package testutils.fixture

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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
  private val minDateTime = LocalDateTime.of(1980, 1, 1, 0, 0)
  private val maxDateTime = LocalDateTime.of(2200, 1, 1, 0, 0)

  val values = Seq(
    Event(
      1,
      Some("description"),
      LocalDateTime.of(2017, 1, 2, 12, 30),
      LocalDateTime.of(2017, 1, 5, 12, 30),
      Seq(
        Event.NotificationTime(
          LocalDateTime.of(2017, 1, 2, 12, 0),
          PreBegin,
          Respondent
        ),
        Event.NotificationTime(
          LocalDateTime.of(2017, 1, 2, 12, 30),
          Begin,
          Respondent
        ),
        Event.NotificationTime(
          LocalDateTime.of(2017, 1, 5, 12, 30),
          End,
          Auditor
        )
      )
    ),
    Event(
      2,
      Some("completed"),
      minDateTime,
      minDateTime.plus(500, ChronoUnit.SECONDS),
      Nil
    ),
    Event(
      3,
      Some("notStarted"),
      maxDateTime.minus(500, ChronoUnit.SECONDS),
      maxDateTime,
      Nil,
      isPreparing = true
    ),
    Event(
      4,
      Some("inProgress"),
      minDateTime,
      maxDateTime,
      Nil
    )
  )
}
