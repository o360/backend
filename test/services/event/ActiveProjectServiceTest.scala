package services.event

import models.ListWithTotal
import models.dao.ActiveProjectDao
import models.project.{ActiveProject, Project}
import models.user.User
import org.mockito.Mockito._
import services.BaseServiceTest
import testutils.generator.{ActiveProjectGenerator, ProjectGenerator, UserGenerator}
import utils.errors.NotFoundError
import utils.listmeta.ListMeta

/**
  * Test for active project service.
  */
class ActiveProjectServiceTest
  extends BaseServiceTest
  with ActiveProjectGenerator
  with UserGenerator
  with ProjectGenerator {

  private case class Fixture(
    activeProjectDao: ActiveProjectDao,
    service: ActiveProjectService
  )

  private def getFixture = {
    val activeProjectDao = mock[ActiveProjectDao]
    val service = new ActiveProjectService(activeProjectDao, ec)
    Fixture(activeProjectDao, service)
  }

  "getList" should {
    "return list of projects with user related info" in {
      forAll { (eventId: Long, user: User, activeProject: ActiveProject, isAuditor: Boolean) =>
        val fixture = getFixture

        when(
          fixture.activeProjectDao.getList(
            optId = *,
            optUserId = eqTo(Some(user.id)),
            optEventId = eqTo(Some(eventId))
          )(*)
        ).thenReturn(toFuture(ListWithTotal(Seq(activeProject))))

        when(fixture.activeProjectDao.isAuditor(activeProject.id, user.id)).thenReturn(toFuture(isAuditor))

        val result = wait(fixture.service.getList(Some(eventId))(user, ListMeta.default).run)

        val expectedProject = activeProject.copy(userInfo = Some(ActiveProject.UserInfo(isAuditor)))

        result mustBe right
        result.toOption.get mustBe ListWithTotal(Seq(expectedProject))
      }
    }
  }

  "create" should {
    "create active project from project" in {
      forAll { (project: Project, eventId: Long) =>
        val fixture = getFixture
        val captor = argumentCaptor[ActiveProject]
        when(fixture.activeProjectDao.create(captor.capture)).thenAnswer(_ => toFuture(captor.getValue))

        val result = wait(fixture.service.create(project, eventId).run)

        result mustBe right
        val createdAp = result.toOption.get

        captor.getValue mustBe createdAp
        createdAp.eventId mustBe eventId
        createdAp.name mustBe project.name
        createdAp.description mustBe project.description
        createdAp.formsOnSamePage mustBe project.formsOnSamePage
        createdAp.canRevote mustBe project.canRevote
        createdAp.isAnonymous mustBe project.isAnonymous
        createdAp.machineName mustBe project.machineName
        createdAp.parentProjectId mustBe Some(project.id)
      }
    }
  }

  "getById" should {
    "return error if project not found" in {
      forAll { (apId: Long, user: User) =>
        val fixture = getFixture

        when(
          fixture.activeProjectDao.getList(
            optId = eqTo(Some(apId)),
            optUserId = eqTo(Some(user.id)),
            optEventId = *
          )(*)
        ).thenReturn(toFuture(ListWithTotal(Seq.empty[ActiveProject])))

        val result = wait(fixture.service.getById(apId)(user).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return active project by ID" in {
      forAll { (apId: Long, user: User, ap: ActiveProject, isAuditor: Boolean) =>
        val fixture = getFixture

        when(
          fixture.activeProjectDao.getList(
            optId = eqTo(Some(apId)),
            optUserId = eqTo(Some(user.id)),
            optEventId = *
          )(*)
        ).thenReturn(toFuture(ListWithTotal(Seq(ap))))
        when(fixture.activeProjectDao.isAuditor(ap.id, user.id)).thenReturn(toFuture(isAuditor))

        val result = wait(fixture.service.getById(apId)(user).run)

        val expectedProject = ap.copy(userInfo = Some(ActiveProject.UserInfo(isAuditor)))

        result mustBe right
        result.toOption.get mustBe expectedProject
      }
    }
  }

  "createProjectAuditors" should {
    "add auditors to project" in {
      forAll { (apId: Long, auditorsIds: Seq[Long]) =>
        val fixture = getFixture
        when(fixture.activeProjectDao.addAuditor(eqTo(apId), *)).thenReturn(toFuture(()))

        val result = wait(fixture.service.createProjectAuditors(apId, auditorsIds).run)

        result mustBe right
      }
    }
  }
}
