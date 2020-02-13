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

import models.EntityKind
import models.competence.Competence
import org.scalacheck.Gen
import testutils.fixture.CompetenceFixture
import testutils.generator.CompetenceGenerator

/**
  * Test for competence DAO.
  */
class CompetenceDaoTest extends BaseDaoTest with CompetenceFixture with CompetenceGenerator {

  private val dao = inject[CompetenceDao]

  "get" should {
    "return competences by specific criteria" in {
      forAll(
        Gen.option(Gen.someOf(Competences.map(_.id))),
        Gen.option(Gen.oneOf[EntityKind](EntityKind.Template, EntityKind.Freezed)),
        Gen.option(Gen.oneOf(CompetenceGroups.map(_.id) :+ -10L))
      ) { (ids, kind, groupId) =>
        val competences = wait(dao.getList(groupId, kind, ids.map(_.toSeq)))
        val expectedCompetences = Competences.filter(c =>
          ids.forall(_.contains(c.id)) && kind.forall(_ == c.kind) && groupId.forall(_ == c.groupId)
        )
        competences.total mustBe expectedCompetences.length
        competences.data must contain theSameElementsAs expectedCompetences
      }
    }
  }

  "findById" should {
    "return competence by ID" in {
      forAll(Gen.oneOf(Competences.map(_.id) :+ -10L)) { id =>
        val competence = wait(dao.getById(id))
        val expectedCompetence = Competences.find(_.id == id)

        competence mustBe expectedCompetence
      }
    }
  }

  "create" should {
    "create competence" in {
      forAll { (competence: Competence) =>
        val created = wait(dao.create(competence))

        val competenceFromDb = wait(dao.getById(created.id))
        competenceFromDb mustBe defined
        created mustBe competenceFromDb.get
      }
    }
  }

  "delete" should {
    "delete competence" in {
      forAll { (competence: Competence) =>
        val created = wait(dao.create(competence))

        wait(dao.delete(created.id))

        val competenceFromDb = wait(dao.getById(created.id))
        competenceFromDb mustBe empty
      }
    }
  }
  "update" should {
    "update competence" in {
      val newCompetenceId = wait(dao.create(Competences(0))).id

      forAll { (competence: Competence) =>
        val competenceWithId = competence.copy(id = newCompetenceId)

        wait(dao.update(competenceWithId))

        val updatedFromDb = wait(dao.getById(newCompetenceId))

        updatedFromDb mustBe defined
        updatedFromDb.get mustBe competenceWithId
      }
    }
  }
}
