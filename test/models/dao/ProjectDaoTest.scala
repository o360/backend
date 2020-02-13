/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.dao

import models.project.Project
import org.scalacheck.Gen
import testutils.fixture.{EventProjectFixture, ProjectFixture}
import testutils.generator.ProjectGenerator

/**
  * Test for project DAO.
  */
class ProjectDaoTest extends BaseDaoTest with ProjectFixture with ProjectGenerator with EventProjectFixture {

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

    "return projects filtered by event" in {
      forAll { (eventId: Option[Long]) =>
        val projects = wait(dao.getList(optEventId = eventId))
        val expectedProjects = Projects
          .filter(p => eventId.forall(e => EventProjects.filter(_._1 == e).map(_._2).contains(p.id)))

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
    "create project" in {
      forAll(Gen.oneOf(Projects)) { (project: Project) =>
        val withUniqName = project.copy(name = java.util.UUID.randomUUID.toString)
        val created = wait(dao.create(withUniqName))

        val projectFromDb = wait(dao.findById(created.id))
        projectFromDb mustBe defined
        created mustBe projectFromDb.get
      }
    }
  }

  "delete" should {
    "delete project" in {
      forAll(Gen.oneOf(Projects)) { (project: Project) =>
        val withUniqName = project.copy(name = java.util.UUID.randomUUID.toString)
        val created = wait(dao.create(withUniqName))

        val rowsDeleted = wait(dao.delete(created.id))

        val projectFromDb = wait(dao.findById(created.id))
        rowsDeleted mustBe 1
        projectFromDb mustBe empty
      }
    }
  }
  "update" should {
    "update project" in {
      val newProjectId = wait(dao.create(Projects(0).copy(name = java.util.UUID.randomUUID.toString))).id

      forAll(Gen.oneOf(Projects)) { (project: Project) =>
        val projectWithId = project.copy(id = newProjectId, name = java.util.UUID.randomUUID.toString)

        wait(dao.update(projectWithId))

        val updatedFromDb = wait(dao.findById(newProjectId))

        updatedFromDb mustBe defined
        updatedFromDb.get mustBe projectWithId
      }
    }
  }
}
