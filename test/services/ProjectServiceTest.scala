package services

import java.sql.SQLException

import models.ListWithTotal
import models.dao.ProjectDao
import models.project.Project
import models.user.User
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import testutils.fixture.ProjectFixture
import testutils.generator.ProjectGenerator
import utils.errors.{ConflictError, NotFoundError}
import utils.listmeta.ListMeta

import scala.concurrent.Future

/**
  * Test for project service.
  */
class ProjectServiceTest extends BaseServiceTest with ProjectGenerator with ProjectFixture {

  private val admin = User(1, None, None, User.Role.Admin, User.Status.Approved)

  private case class TestFixture(
    projectDaoMock: ProjectDao,
    service: ProjectService)

  private def getFixture = {
    val daoMock = mock[ProjectDao]
    val service = new ProjectService(daoMock)
    TestFixture(daoMock, service)
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
      projects: Seq[Project],
      total: Int
      ) =>
        val fixture = getFixture
        when(fixture.projectDaoMock.getList(optId = any[Option[Long]])(eqTo(ListMeta.default)))
          .thenReturn(toFuture(ListWithTotal(total, projects)))
        val result = wait(fixture.service.getList()(admin, ListMeta.default).run)

        result mustBe 'right
        result.toOption.get mustBe ListWithTotal(total, projects)

        verify(fixture.projectDaoMock, times(1)).getList(
          optId = any[Option[Long]]
        )(eqTo(ListMeta.default))
      }
    }
  }

  "create" should {
    "return conflict if db exception" in {
      forAll { (project: Project) =>
        val fixture = getFixture
        when(fixture.projectDaoMock.create(project.copy(id = 0))).thenReturn(Future.failed(new SQLException("", "2300")))
        val result = wait(fixture.service.create(project.copy(id = 0))(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[ConflictError]
      }
    }

    "create project in db" in {
      forAll { (project: Project) =>
        val fixture = getFixture
        when(fixture.projectDaoMock.create(project.copy(id = 0))).thenReturn(toFuture(project))
        val result = wait(fixture.service.create(project.copy(id = 0))(admin).run)

        result mustBe 'right
        result.toOption.get mustBe project
      }
    }
  }

  "update" should {
    "return conflict if db exception" in {
      forAll { (project: Project) =>
        val fixture = getFixture
        when(fixture.projectDaoMock.findById(project.id)).thenReturn(toFuture(Some(project)))
        when(fixture.projectDaoMock.update(project.copy(id = 0))).thenReturn(Future.failed(new SQLException("", "2300")))
        val result = wait(fixture.service.update(project.copy(id = 0))(admin).run)

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
      forAll { (project: Project) =>
        val fixture = getFixture
        when(fixture.projectDaoMock.findById(project.id)).thenReturn(toFuture(Some(project)))
        when(fixture.projectDaoMock.update(project)).thenReturn(toFuture(project))
        val result = wait(fixture.service.update(project)(admin).run)

        result mustBe 'right
        result.toOption.get mustBe project
      }
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
