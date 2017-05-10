package services

import models.dao.EventProjectDao
import models.event.Event
import models.project.Project
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito._
import testutils.fixture.{UserFixture, EventProjectFixture}
import testutils.generator.TristateGenerator
import utils.errors.{ApplicationError, NotFoundError}

import scalaz.{-\/, EitherT, \/, \/-}

/**
  * Test for project-event service.
  */
class EventProjectServiceTest extends BaseServiceTest with TristateGenerator with EventProjectFixture {

  private val admin = UserFixture.admin

  private case class TestFixture(
    eventProjectDaoMock: EventProjectDao,
    projectServiceMock: ProjectService,
    eventServiceMock: EventService,
    service: EventProjectService)

  private def getFixture = {
    val eventProjectDao = mock[EventProjectDao]
    val projectService = mock[ProjectService]
    val eventService = mock[EventService]
    val service = new EventProjectService(projectService, eventService, eventProjectDao)
    TestFixture(eventProjectDao, projectService, eventService, service)
  }

  "add" should {
    "return error if project not found" in {
      forAll { (eventId: Long, projectId: Long) =>
        val fixture = getFixture
        when(fixture.projectServiceMock.getById(projectId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(-\/(NotFoundError.Project(projectId)): ApplicationError \/ Project)))
        val result = wait(fixture.service.add(eventId, projectId)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return error if event not found" in {
      forAll { (eventId: Long, projectId: Long) =>
        val fixture = getFixture
        when(fixture.projectServiceMock.getById(projectId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Projects(0)):  ApplicationError \/ Project)))
        when(fixture.eventServiceMock.getById(eventId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(-\/(NotFoundError.Event(eventId)):  ApplicationError \/ Event)))
        val result = wait(fixture.service.add(eventId, projectId)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "not add if project already in event" in {
      forAll { (eventId: Long, projectId: Long) =>
        val fixture = getFixture
        when(fixture.projectServiceMock.getById(projectId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Projects(0)):  ApplicationError \/ Project)))
        when(fixture.eventServiceMock.getById(eventId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Events(0)):  ApplicationError \/ Event)))

        when(fixture.eventProjectDaoMock.exists(eventId = eqTo(Some(eventId)), projectId = eqTo(Some(projectId))))
          .thenReturn(toFuture(true))
        val result = wait(fixture.service.add(eventId, projectId)(admin).run)

        result mustBe 'right
        verify(fixture.eventProjectDaoMock, times(1)).exists(eventId = Some(eventId), projectId = Some(projectId))
      }
    }

    "add project to event" in {
      forAll { (eventId: Long, projectId: Long) =>
        val fixture = getFixture
        when(fixture.projectServiceMock.getById(projectId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Projects(0)):  ApplicationError \/ Project)))
        when(fixture.eventServiceMock.getById(eventId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Events(0)):  ApplicationError \/ Event)))
        when(fixture.eventProjectDaoMock.exists(eventId = eqTo(Some(eventId)), projectId = eqTo(Some(projectId))))
          .thenReturn(toFuture(false))
        when(fixture.eventProjectDaoMock.add(eventId, projectId)).thenReturn(toFuture(()))
        val result = wait(fixture.service.add(eventId, projectId)(admin).run)

        result mustBe 'right
        verify(fixture.eventProjectDaoMock, times(1)).exists(eventId = Some(eventId), projectId = Some(projectId))
        verify(fixture.eventProjectDaoMock, times(1)).add(eventId, projectId)
      }
    }
  }

  "remove" should {
    "return error if project not found" in {
      forAll { (eventId: Long, projectId: Long) =>
        val fixture = getFixture
        when(fixture.projectServiceMock.getById(projectId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(-\/(NotFoundError.Project(projectId)):  ApplicationError \/ Project)))
        val result = wait(fixture.service.remove(eventId, projectId)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return error if event not found" in {
      forAll { (eventId: Long, projectId: Long) =>
        val fixture = getFixture
        when(fixture.projectServiceMock.getById(projectId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Projects(0)):  ApplicationError \/ Project)))
        when(fixture.eventServiceMock.getById(eventId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(-\/(NotFoundError.Event(eventId)):  ApplicationError \/ Event)))
        val result = wait(fixture.service.remove(eventId, projectId)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "remove project from event" in {
      forAll { (eventId: Long, projectId: Long) =>
        val fixture = getFixture
        when(fixture.projectServiceMock.getById(projectId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Projects(0)):  ApplicationError \/ Project)))
        when(fixture.eventServiceMock.getById(eventId)(admin))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Events(0)):  ApplicationError \/ Event)))
        when(fixture.eventProjectDaoMock.remove(eventId, projectId)).thenReturn(toFuture(()))
        val result = wait(fixture.service.remove(eventId, projectId)(admin).run)

        result mustBe 'right
        verify(fixture.eventProjectDaoMock, times(1)).remove(eventId, projectId)
      }
    }
  }
}

