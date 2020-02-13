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

package models.dao

import java.time.{LocalDateTime, ZoneOffset}
import java.time.temporal.ChronoUnit

import models.event.EventJob
import testutils.fixture.EventJobFixture
import testutils.generator.EventJobGenerator

/**
  * Test for event job DAO.
  */
class EventJobDaoTest extends BaseDaoTest with EventJobFixture with EventJobGenerator {

  val dao = inject[EventJobDao]

  implicit val localDateTimeOrdering: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(ZoneOffset.UTC))

  "getJobs" should {
    "return all jobs for status" in {
      forAll { (status: EventJob.Status) =>
        val result = wait(
          dao.getJobs(
            EventJobs.map(_.time).min.minusDays(1),
            EventJobs.map(_.time).max.plusDays(1),
            status
          )
        )

        val expectedResult = EventJobs.filter(_.status == status)

        result must contain theSameElementsAs expectedResult
      }
    }

    "fitler jobs by time" in {
      val firstJob = EventJobs(0)
      val timeFrom = firstJob.time.minus(1, ChronoUnit.MILLIS)
      val timeTo = firstJob.time.plus(1, ChronoUnit.MILLIS)
      val result = wait(dao.getJobs(timeFrom, timeTo, firstJob.status))

      result.length mustBe 1
      result.head mustBe firstJob
    }
  }

  "createJob" should {
    "create job" in {
      forAll { (job: EventJob) =>
        val eventId = Events(0).id
        val preparedJob = job.copyWith(eventId = eventId)
        wait(dao.createJob(preparedJob))

        val timeFrom = preparedJob.time.minus(1, ChronoUnit.MILLIS)
        val timeTo = preparedJob.time.plus(1, ChronoUnit.MILLIS)

        val fromDb = wait(dao.getJobs(timeFrom, timeTo, preparedJob.status))

        fromDb.length mustBe 1
        fromDb.head.copyWith(id = 0) mustBe preparedJob
      }
    }
  }

  "updateStatus" should {
    "update status" in {
      val job = EventJobs(0)
      forAll { (status: EventJob.Status) =>
        wait(dao.updateStatus(job.id, status))

        val timeFrom = job.time.minus(1, ChronoUnit.MILLIS)
        val timeTo = job.time.plus(1, ChronoUnit.MILLIS)

        val fromDb = wait(dao.getJobs(timeFrom, timeTo, status))

        fromDb.length mustBe 1
        fromDb.head mustBe job.copyWith(status = status)
      }
    }
  }
}
