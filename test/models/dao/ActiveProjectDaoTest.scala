package models.dao

import models.project.ActiveProject
import org.scalacheck.Gen
import testutils.fixture.{ActiveProjectFixture, AnswerFixture, UserFixture}
import testutils.generator.ActiveProjectGenerator

/**
  * Test for activeProject DAO.
  */
class ActiveProjectDaoTest
  extends BaseDaoTest
  with ActiveProjectFixture
  with ActiveProjectGenerator
  with AnswerFixture {

  private val dao = inject[ActiveProjectDao]

  "get" should {
    "return active projects by specific criteria" in {
      forAll(Gen.option(Gen.choose(0L, 3L))) { id =>
        val activeProjects = wait(dao.getList(id))
        val expectedActiveProjects = ActiveProjectFixture.values.filter(u => id.forall(_ == u.id))
        activeProjects.total mustBe expectedActiveProjects.length
        activeProjects.data must contain theSameElementsAs expectedActiveProjects
      }
    }

    "return active projects filtered by user" in {
      forAll(Gen.option(Gen.oneOf(UserFixture.values.map(_.id)))) { userId =>
        val activeProjects = wait(dao.getList(optUserId = userId))
        val expectedActiveProjects = ActiveProjectFixture.values
          .filter { p =>
            userId.forall(uid =>
              AnswerFixture.values
                .filter(_.userFromId == uid)
                .map(_.activeProjectId)
                .contains(p.id)
            )
          }

        activeProjects.data must contain theSameElementsAs expectedActiveProjects
      }
    }
  }

  "create" should {
    "create active project" in {
      forAll { activeProject: ActiveProject =>
        val created = wait(dao.create(activeProject))

        val activeProjectFromDb = wait(dao.getList(optId = Some(created.id))).data.headOption
        activeProjectFromDb mustBe defined
        created mustBe activeProjectFromDb.get
      }
    }
  }

  "isAuditor" should {
    "return true if user is auditor of the projects" in {
      forAll(
        Gen.oneOf(UserFixture.values.map(_.id)),
        Gen.oneOf(ActiveProjectFixture.values.map(_.id))
      ) { (userId, apId) =>
        val isAuditor = wait(dao.isAuditor(apId, userId))
        val expectedIsAuditor =
          ActiveProjectFixture.auditorValues.exists(x => x.userId == userId && x.projectId == apId)

        isAuditor mustBe expectedIsAuditor
      }
    }
  }

  "addAuditor" should {
    "add auditor to project" in {
      forAll(
        Gen.oneOf(UserFixture.values.map(_.id)),
        Gen.oneOf(ActiveProjectFixture.values.map(_.id))
      ) { (userId, apId) =>
        val isAuditor = wait(dao.isAuditor(apId, userId))

        if (!isAuditor) {
          wait(dao.addAuditor(apId, userId))
          val newIsAuditor = wait(dao.isAuditor(apId, userId))
          newIsAuditor mustBe true
        }
      }
    }
  }
}
