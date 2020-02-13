/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package testutils.fixture

import java.time.LocalDateTime

import com.ninja_squad.dbsetup.Operations.insertInto
import models.event.{Event, EventJob}
import models.notification._

/**
  * Fixture for event job.
  */
trait EventJobFixture extends FixtureHelper with EventFixture { _: FixtureSupport =>

  val EventJobs: Seq[EventJob] = Seq(
    EventJob.Upload(1, 1, LocalDateTime.of(1985, 1, 1, 0, 5), EventJob.Status.Cancelled),
    EventJob.Upload(2, 2, LocalDateTime.of(1985, 1, 2, 1, 5), EventJob.Status.New),
    EventJob.SendNotification(
      3,
      1,
      Event.NotificationTime(LocalDateTime.of(1986, 1, 1, 0, 5), PreBegin, Respondent),
      EventJob.Status.Failure
    )
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

object EventJobFixture {
  val eventStart = EventJob.EventStart(0, 0, LocalDateTime.of(2000, 1, 1, 0, 0), EventJob.Status.New)
}
