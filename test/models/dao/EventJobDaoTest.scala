package models.dao

import java.sql.Timestamp

import models.event.EventJob
import testutils.fixture.EventJobFixture
import testutils.generator.EventJobGenerator

/**
  * Test for event job DAO.
  */
class EventJobDaoTest extends BaseDaoTest with EventJobFixture with EventJobGenerator {

  val dao = inject[EventJobDao]

  "getJobs" should {
    "return all jobs for status" in {
      forAll { (status: EventJob.Status) =>
        val result = wait(dao.getJobs(new Timestamp(0), new Timestamp(Long.MaxValue), status))

        val expectedResult = EventJobs.filter(_.status == status)

        result must contain theSameElementsAs expectedResult
      }
    }

    "fitler jobs by time" in {
      val firstJob = EventJobs(0)
      val timeFrom = new Timestamp(firstJob.time.getTime - 5)
      val timeTo = new Timestamp(firstJob.time.getTime + 5)
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

        val timeFrom = new Timestamp(preparedJob.time.getTime - 5)
        val timeTo = new Timestamp(preparedJob.time.getTime + 5)

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

        val timeFrom = new Timestamp(job.time.getTime - 5)
        val timeTo = new Timestamp(job.time.getTime + 5)

        val fromDb = wait(dao.getJobs(timeFrom, timeTo, status))

        fromDb.length mustBe 1
        fromDb.head mustBe job.copyWith(status = status)
      }
    }
  }
}
