package services.event

import models.dao.EventProjectDao
import models.event.Event
import models.project.Project
import org.mockito.Mockito._
import services.{BaseServiceTest, ProjectService}
import testutils.fixture.EventProjectFixture
import testutils.generator.TristateGenerator
import utils.errors.NotFoundError

/**
  * Test for project-event service.
  */
class EventProjectServiceTest extends BaseServiceTest with TristateGenerator with EventProjectFixture {

  private case class TestFixture(
    eventProjectDaoMock: EventProjectDao,
    projectService: ProjectService,
    eventServiceMock: EventService,
    service: EventProjectService
  )

  private def getFixture = {
    val eventProjectDao = mock[EventProjectDao]
    val projectService = mock[ProjectService]
    val eventService = mock[EventService]
    val service = new EventProjectService(projectService, eventService, eventProjectDao, ec)
    TestFixture(eventProjectDao, projectService, eventService, service)
  }

  "add" should {
    "return error if project not found" in {
      forAll { (eventId: Long, projectId: Long) =>
        val fixture = getFixture
        when(fixture.projectService.getById(projectId))
          .thenReturn(toErrorResult[Project](NotFoundError.Project(projectId)))
        val result = wait(fixture.service.add(eventId, projectId).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return error if event not found" in {
      forAll { (eventId: Long, projectId: Long) =>
        val fixture = getFixture
        when(fixture.projectService.getById(projectId)).thenReturn(toSuccessResult(Projects(0)))
        when(fixture.eventServiceMock.getById(eventId)).thenReturn(toErrorResult[Event](NotFoundError.Event(eventId)))
        val result = wait(fixture.service.add(eventId, projectId).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "not add if project already in event" in {
      forAll { (eventId: Long, projectId: Long) =>
        val fixture = getFixture
        when(fixture.projectService.getById(projectId)).thenReturn(toSuccessResult(Projects(0)))
        when(fixture.eventServiceMock.getById(eventId)).thenReturn(toSuccessResult(Events(0)))
        when(fixture.eventProjectDaoMock.exists(eventId = eqTo(Some(eventId)), projectId = eqTo(Some(projectId))))
          .thenReturn(toFuture(true))
        val result = wait(fixture.service.add(eventId, projectId).run)

        result mustBe right
        verify(fixture.eventProjectDaoMock, times(1)).exists(eventId = Some(eventId), projectId = Some(projectId))
      }
    }

    "add project to event" in {
      forAll { (eventId: Long, projectId: Long) =>
        val fixture = getFixture
        when(fixture.projectService.getById(projectId)).thenReturn(toSuccessResult(Projects(0)))
        when(fixture.eventServiceMock.getById(eventId)).thenReturn(toSuccessResult(Events(0)))
        when(fixture.eventProjectDaoMock.exists(eventId = eqTo(Some(eventId)), projectId = eqTo(Some(projectId))))
          .thenReturn(toFuture(false))
        when(fixture.eventProjectDaoMock.add(eventId, projectId)).thenReturn(toFuture(()))
        val result = wait(fixture.service.add(eventId, projectId).run)

        result mustBe right
        verify(fixture.eventProjectDaoMock, times(1)).exists(eventId = Some(eventId), projectId = Some(projectId))
        verify(fixture.eventProjectDaoMock, times(1)).add(eventId, projectId)
      }
    }
  }

  "remove" should {
    "return error if project not found" in {
      forAll { (eventId: Long, projectId: Long) =>
        val fixture = getFixture
        when(fixture.projectService.getById(projectId))
          .thenReturn(toErrorResult[Project](NotFoundError.Project(projectId)))
        val result = wait(fixture.service.remove(eventId, projectId).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return error if event not found" in {
      forAll { (eventId: Long, projectId: Long) =>
        val fixture = getFixture
        when(fixture.projectService.getById(projectId)).thenReturn(toSuccessResult(Projects(0)))
        when(fixture.eventServiceMock.getById(eventId)).thenReturn(toErrorResult[Event](NotFoundError.Event(eventId)))
        val result = wait(fixture.service.remove(eventId, projectId).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "remove project from event" in {
      forAll { (eventId: Long, projectId: Long) =>
        val fixture = getFixture
        when(fixture.projectService.getById(projectId)).thenReturn(toSuccessResult(Projects(0)))
        when(fixture.eventServiceMock.getById(eventId)).thenReturn(toSuccessResult(Events(0)))
        when(fixture.eventProjectDaoMock.remove(eventId, projectId)).thenReturn(toFuture(()))
        val result = wait(fixture.service.remove(eventId, projectId).run)

        result mustBe right
        verify(fixture.eventProjectDaoMock, times(1)).remove(eventId, projectId)
      }
    }
  }
}
