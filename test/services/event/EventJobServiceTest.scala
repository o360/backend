package services

import java.time.LocalDateTime

import models.dao.{EventDao, EventJobDao}
import models.event.{Event, EventJob}
import models.notification._
import org.mockito.Mockito._
import services.event.{EventJobService, EventStartService}
import testutils.fixture.EventFixture
import testutils.generator.EventJobGenerator
import utils.errors.NotFoundError

/**
  * Test for event job service.
  */
class EventJobServiceTest extends BaseServiceTest with EventFixture with EventJobGenerator {

  private case class Fixture(
    eventJobDao: EventJobDao,
    eventDao: EventDao,
    notificationService: NotificationService,
    uploadService: UploadService,
    eventStartService: EventStartService,
    service: EventJobService
  )

  private def getFixture = {
    val eventJobDao = mock[EventJobDao]
    val eventDao = mock[EventDao]
    val notificationService = mock[NotificationService]
    val uploadService = mock[UploadService]
    val eventStartService = mock[EventStartService]
    val service = new EventJobService(eventJobDao, eventDao, notificationService, uploadService, eventStartService, ec)
    Fixture(eventJobDao, eventDao, notificationService, uploadService, eventStartService, service)
  }

  "create" should {
    "create jobs in DB" in {
      val fixture = getFixture

      val event = Events(0).copy(
        notifications = Seq(
          Event.NotificationTime(
            LocalDateTime.of(2040, 1, 2, 12, 0),
            PreBegin,
            Respondent
          ),
          Event.NotificationTime(
            LocalDateTime.of(0, 1, 2, 12, 30),
            Begin,
            Respondent
          )
        ),
        end = LocalDateTime.of(2040, 1, 2, 11, 4)
      )

      val job1 = EventJob.Upload(0, event.id, event.end, EventJob.Status.New)
      val job2 = EventJob.SendNotification(0, event.id, event.notifications(0), EventJob.Status.New)
      val job3 = EventJob.EventStart(0, event.id, event.start, EventJob.Status.New)

      when(fixture.eventJobDao.createJob(job1)).thenReturn(toFuture(()))
      when(fixture.eventJobDao.createJob(job2)).thenReturn(toFuture(()))
      when(fixture.eventJobDao.createJob(job3)).thenReturn(toFuture(()))

      wait(fixture.service.createJobs(event))

      succeed
    }
  }

  "get" should {
    "cancel non actual jobs" in {
      val fixture = getFixture
      val from = LocalDateTime.of(1980, 1, 1, 5, 0)
      val to = LocalDateTime.of(1980, 1, 2, 5, 0)

      val event = Events(0).copy(notifications = Seq(), end = LocalDateTime.of(2040, 1, 2, 11, 4))

      val nonActualJob1 = EventJob.Upload(1, event.id, from, EventJob.Status.New)
      val nonActualJob2 = EventJob.SendNotification(2, 789, Events(0).notifications(0), EventJob.Status.New)
      val nonActualJob3 = EventJob.SendNotification(3, event.id, Events(0).notifications(0), EventJob.Status.New)
      val jobs = Seq(nonActualJob1, nonActualJob2, nonActualJob3)

      when(fixture.eventJobDao.getJobs(from, to, EventJob.Status.New))
        .thenReturn(toFuture(jobs))
      when(fixture.eventDao.findById(event.id)).thenReturn(toFuture(Some(event)))
      when(fixture.eventDao.findById(789)).thenReturn(toFuture(None))
      when(fixture.eventJobDao.updateStatus(1, EventJob.Status.Cancelled)).thenReturn(toFuture(()))
      when(fixture.eventJobDao.updateStatus(2, EventJob.Status.Cancelled)).thenReturn(toFuture(()))
      when(fixture.eventJobDao.updateStatus(3, EventJob.Status.Cancelled)).thenReturn(toFuture(()))

      val result = wait(fixture.service.get(from, to))

      result mustBe empty
    }

    "return actual jobs" in {
      val fixture = getFixture
      val from = LocalDateTime.of(1980, 1, 1, 5, 0)
      val to = LocalDateTime.of(1980, 1, 2, 5, 0)

      val event = Events(0)

      val actualJob1 = EventJob.Upload(1, event.id, event.end, EventJob.Status.New)
      val actualJob2 = EventJob.SendNotification(2, event.id, event.notifications(0), EventJob.Status.New)

      val jobs = Seq(actualJob1, actualJob2)

      when(fixture.eventJobDao.getJobs(from, to, EventJob.Status.New))
        .thenReturn(toFuture(jobs))
      when(fixture.eventDao.findById(event.id)).thenReturn(toFuture(Some(event)))

      val result = wait(fixture.service.get(from, to))

      result mustBe jobs
    }
  }

  "execute" should {
    "execute jobs" in {
      val fixture = getFixture

      val event = Events(0)

      val actualJob1 = EventJob.Upload(1, event.id, event.end, EventJob.Status.New)
      val actualJob2 = EventJob.SendNotification(2, event.id, event.notifications(0), EventJob.Status.New)
      val actualJob3 = EventJob.EventStart(3, event.id, event.start, EventJob.Status.New)

      val jobs = Seq(actualJob1, actualJob2, actualJob3)

      when(fixture.uploadService.execute(actualJob1)).thenReturn(toFuture(()))
      when(fixture.notificationService.execute(actualJob2)).thenReturn(toFuture(()))
      when(fixture.eventStartService.execute(actualJob3)).thenReturn(toFuture(()))
      when(fixture.eventJobDao.updateStatus(1, EventJob.Status.Success)).thenReturn(toFuture(()))
      when(fixture.eventJobDao.updateStatus(2, EventJob.Status.Success)).thenReturn(toFuture(()))
      when(fixture.eventJobDao.updateStatus(3, EventJob.Status.Success)).thenReturn(toFuture(()))

      fixture.service.execute(jobs)

      succeed
    }
  }

  "runFailedJob" should {
    "return not found if job not found or not in failure status" in {
      forAll { (id: Long, maybeJob: Option[EventJob]) =>
        whenever(!maybeJob.map(_.status).contains(EventJob.Status.Failure)) {
          val fixture = getFixture

          when(fixture.eventJobDao.find(id)).thenReturn(toFuture(maybeJob))

          val result = wait(fixture.service.runFailedJob(id).run)

          result mustBe left
          result.swap.toOption.get mustBe a[NotFoundError]
        }
      }
    }

    "execute failed job" in {
      forAll { (id: Long, job: EventJob) =>
        whenever(job.status == EventJob.Status.Failure) {
          val fixture = getFixture

          when(fixture.eventJobDao.find(id)).thenReturn(toFuture(Some(job)))

          val result = wait(fixture.service.runFailedJob(id).run)

          result mustBe right
        }
      }
    }
  }
}
