package services

import java.sql.{SQLException, Timestamp}

import models.ListWithTotal
import models.dao.{EventDao, ProjectDao}
import models.event.Event
import models.project.Project
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import testutils.fixture.{ProjectFixture, UserFixture}
import testutils.generator.ProjectGenerator
import utils.errors.{ConflictError, NotFoundError}
import utils.listmeta.ListMeta

import scala.concurrent.Future

/**
  * Test for project service.
  */
class ProjectServiceTest extends BaseServiceTest with ProjectGenerator with ProjectFixture {

  private val admin = UserFixture.admin

  private case class TestFixture(
    projectDaoMock: ProjectDao,
    eventDaoMock: EventDao,
    service: ProjectService)

  private def getFixture = {
    val daoMock = mock[ProjectDao]
    val eventDaoMock = mock[EventDao]
    val service = new ProjectService(daoMock, eventDaoMock)
    TestFixture(daoMock, eventDaoMock, service)
  }

  "getById" should {

    "return not found if project not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.projectDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.getById(id)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.projectDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.projectDaoMock)
      }
    }

    "return project from db" in {
      forAll { (project: Project, id: Long) =>
        val fixture = getFixture
        when(fixture.projectDaoMock.findById(id)).thenReturn(toFuture(Some(project)))
        val result = wait(fixture.service.getById(id)(admin).run)

        result mustBe 'right
        result.toOption.get mustBe project

        verify(fixture.projectDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.projectDaoMock)
      }
    }
  }

  "list" should {
    "return list of projects from db" in {
      forAll { (
      eventId: Option[Long],
      projects: Seq[Project],
      total: Int
      ) =>
        val fixture = getFixture
        when(fixture.projectDaoMock.getList(optId = any[Option[Long]], optEventId = eqTo(eventId))(eqTo(ListMeta.default)))
          .thenReturn(toFuture(ListWithTotal(total, projects)))
        val result = wait(fixture.service.getList(eventId)(admin, ListMeta.default).run)

        result mustBe 'right
        result.toOption.get mustBe ListWithTotal(total, projects)
      }
    }
  }

  "create" should {
    "return conflict if db exception" in {
      forAll { (project: Project) =>
        val fixture = getFixture
        when(fixture.projectDaoMock.create(any[Project])).thenReturn(Future.failed(new SQLException("", "2300")))
        val result = wait(fixture.service.create(project.copy(id = 0))(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[ConflictError]
      }
    }

    "create project in db" in {
      val project = Projects(0)

      val fixture = getFixture
      when(fixture.projectDaoMock.create(project.copy(id = 0))).thenReturn(toFuture(project))
      val result = wait(fixture.service.create(project.copy(id = 0))(admin).run)

      result mustBe 'right
      result.toOption.get mustBe project
    }
  }

  "update" should {
    "return conflict if db exception" in {
      forAll { (project: Project) =>
        val fixture = getFixture
        when(fixture.projectDaoMock.findById(project.id)).thenReturn(toFuture(Some(project)))
        when(fixture.eventDaoMock.getList(
          optId = any[Option[Long]],
          optStatus = eqTo(Some(Event.Status.InProgress)),
          optProjectId = eqTo(Some(project.id)),
          optNotificationFrom = any[Option[Timestamp]],
          optNotificationTo = any[Option[Timestamp]],
          optFormId = any[Option[Long]]
        )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))
        when(fixture.projectDaoMock.update(any[Project])).thenReturn(Future.failed(new SQLException("", "2300")))
        val result = wait(fixture.service.update(project)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[ConflictError]
      }
    }

    "return conflict if exists events in progress" in {
      forAll { (project: Project) =>
        val fixture = getFixture
        when(fixture.projectDaoMock.findById(project.id)).thenReturn(toFuture(Some(project)))
        when(fixture.eventDaoMock.getList(
          optId = any[Option[Long]],
          optStatus = eqTo(Some(Event.Status.InProgress.asInstanceOf[Event.Status])),
          optProjectId = eqTo(Some(project.id)),
          optNotificationFrom = any[Option[Timestamp]],
          optNotificationTo = any[Option[Timestamp]],
          optFormId = any[Option[Long]]
        )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Event](1, Nil)))
        val result = wait(fixture.service.update(project)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[ConflictError]
      }
    }

    "return not found if project not found" in {
      forAll { (project: Project) =>
        val fixture = getFixture
        when(fixture.projectDaoMock.findById(project.id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.update(project)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.projectDaoMock, times(1)).findById(project.id)
        verifyNoMoreInteractions(fixture.projectDaoMock)
      }
    }

    "update project in db" in {
      val project = Projects(0)
      val fixture = getFixture
      when(fixture.projectDaoMock.findById(project.id)).thenReturn(toFuture(Some(project)))
      when(fixture.eventDaoMock.getList(
        optId = any[Option[Long]],
        optStatus = eqTo(Some(Event.Status.InProgress)),
        optProjectId = eqTo(Some(project.id)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))
      when(fixture.projectDaoMock.update(project)).thenReturn(toFuture(project))
      val result = wait(fixture.service.update(project)(admin).run)

      result mustBe 'right
      result.toOption.get mustBe project
    }
  }

  "delete" should {
    "return not found if project not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.projectDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.delete(id)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.projectDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.projectDaoMock)
      }
    }

    "delete project from db" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.projectDaoMock.findById(id)).thenReturn(toFuture(Some(Projects(0))))
        when(fixture.projectDaoMock.delete(id)).thenReturn(toFuture(1))

        val result = wait(fixture.service.delete(id)(admin).run)

        result mustBe 'right
      }
    }
  }
}
