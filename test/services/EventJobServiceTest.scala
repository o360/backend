package services

import java.sql.Timestamp
import java.time.LocalDateTime

import models.dao.{EventDao, EventJobDao}
import models.event.{Event, EventJob}
import models.notification.Notification
import org.mockito.Mockito._
import testutils.fixture.EventFixture

/**
  * Test for event job service.
  */
class EventJobServiceTest extends BaseServiceTest with EventFixture {

  private case class Fixture(
    eventJobDao: EventJobDao,
    eventDao: EventDao,
    notificationService: NotificationService,
    uploadService: UploadService,
    formService: FormService,
    service: EventJobService
  )

  private def getFixture = {
    val eventJobDao = mock[EventJobDao]
    val eventDao = mock[EventDao]
    val notificationService = mock[NotificationService]
    val uploadService = mock[UploadService]
    val formService = mock[FormService]
    val service = new EventJobService(eventJobDao, eventDao, notificationService, uploadService, formService, ec)
    Fixture(eventJobDao, eventDao, notificationService, uploadService, formService, service)
  }

  "create" should {
    "create jobs in DB" in {
      val fixture = getFixture

      val event = Events(0).copy(
        notifications = Seq(
          Event.NotificationTime(
            Timestamp.valueOf(LocalDateTime.of(2040, 1, 2, 12, 0)),
            Notification.Kind.PreBegin,
            Notification.Recipient.Respondent
          ),
          Event.NotificationTime(
            Timestamp.valueOf(LocalDateTime.of(0, 1, 2, 12, 30)),
            Notification.Kind.Begin,
            Notification.Recipient.Respondent
          )
        ),
        end = Timestamp.valueOf(LocalDateTime.of(2040, 1, 2, 11, 4))
      )

      val job1 = EventJob.Upload(0, event.id, event.end, EventJob.Status.New)
      val job2 = EventJob.SendNotification(0, event.id, event.notifications(0), EventJob.Status.New)
      val job3 = EventJob.CreateFreezedForms(0, event.id, event.start, EventJob.Status.New)

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
      val from = new Timestamp(123)
      val to = new Timestamp(456)

      val event = Events(0).copy(notifications = Seq(), end = Timestamp.valueOf(LocalDateTime.of(2040, 1, 2, 11, 4)))

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
      val from = new Timestamp(123)
      val to = new Timestamp(456)

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
      val actualJob3 = EventJob.CreateFreezedForms(3, event.id, event.start, EventJob.Status.New)

      val jobs = Seq(actualJob1, actualJob2, actualJob3)

      when(fixture.uploadService.execute(actualJob1)).thenReturn(toFuture(()))
      when(fixture.notificationService.execute(actualJob2)).thenReturn(toFuture(()))
      when(fixture.formService.execute(actualJob3)).thenReturn(toFuture(()))
      when(fixture.eventJobDao.updateStatus(1, EventJob.Status.Success)).thenReturn(toFuture(()))
      when(fixture.eventJobDao.updateStatus(2, EventJob.Status.Success)).thenReturn(toFuture(()))
      when(fixture.eventJobDao.updateStatus(3, EventJob.Status.Success)).thenReturn(toFuture(()))

      fixture.service.execute(jobs)

      succeed
    }
  }
}
