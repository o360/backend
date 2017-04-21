package models.dao

import models.project.Project
import org.scalacheck.Gen
import testutils.fixture.ProjectFixture
import testutils.generator.ProjectGenerator

/**
  * Test for project DAO.
  */
class ProjectDaoTest extends BaseDaoTest with ProjectFixture with ProjectGenerator {

  private val dao = inject[ProjectDao]

  "get" should {
    "return projects by specific criteria" in {
      forAll(Gen.option(Gen.choose(0L, 3L))) { (id: Option[Long]) =>
        val projects = wait(dao.getList(id))
        val expectedProjects =
          Projects.filter(u => id.forall(_ == u.id))
        projects.total mustBe expectedProjects.length
        projects.data must contain theSameElementsAs expectedProjects
      }
    }
  }

  "findById" should {
    "return project by ID" in {
      forAll(Gen.choose(0L, Projects.length)) { (id: Long) =>
        val project = wait(dao.findById(id))
        val expectedProject = Projects.find(_.id == id)

        project mustBe expectedProject
      }
    }
  }

  "create" should {
    "create project with relations" in {
      forAll(Gen.oneOf(Projects)) { (project: Project) =>
        val created = wait(dao.create(project))

        val projectFromDb = wait(dao.findById(created.id))
        projectFromDb mustBe defined
        created mustBe projectFromDb.get
      }
    }
  }

  "delete" should {
    "delete project with elements" in {
      forAll(Gen.oneOf(Projects)) { (project: Project) =>
        val created = wait(dao.create(project))

        val rowsDeleted = wait(dao.delete(created.id))

        val projectFromDb = wait(dao.findById(created.id))
        rowsDeleted mustBe 1
        projectFromDb mustBe empty
      }
    }
  }
  "update" should {
    "update project" in {
      val newProjectId = wait(dao.create(Projects(0))).id

      forAll(Gen.oneOf(Projects)) { (project: Project) =>
        val projectWithId = project.copy(id = newProjectId)

        wait(dao.update(projectWithId))

        val updatedFromDb = wait(dao.findById(newProjectId))

        updatedFromDb mustBe defined
        updatedFromDb.get mustBe projectWithId
      }
    }
  }
}
