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
import models.competence.CompetenceGroup
import org.scalacheck.Gen
import testutils.fixture.CompetenceGroupFixture
import testutils.generator.CompetenceGroupGenerator

/**
  * Test for competence group DAO.
  */
class CompetenceGroupDaoTest extends BaseDaoTest with CompetenceGroupFixture with CompetenceGroupGenerator {

  private val dao = inject[CompetenceGroupDao]

  "get" should {
    "return groups by specific criteria" in {
      forAll(
        Gen.option(Gen.someOf(CompetenceGroups.map(_.id))),
        Gen.option(Gen.oneOf[EntityKind](EntityKind.Template, EntityKind.Freezed))
      ) { (ids, kind) =>
        val groups = wait(dao.getList(kind, ids.map(_.toSeq)))
        val expectedGroups = CompetenceGroups.filter(cg => ids.forall(_.contains(cg.id)) && kind.forall(_ == cg.kind))
        groups.total mustBe expectedGroups.length
        groups.data must contain theSameElementsAs expectedGroups
      }
    }
  }

  "findById" should {
    "return group by ID" in {
      forAll(Gen.oneOf(CompetenceGroups.map(_.id) :+ -10L)) { id =>
        val group = wait(dao.getById(id))
        val expectedGroup = CompetenceGroups.find(_.id == id)

        group mustBe expectedGroup
      }
    }
  }

  "create" should {
    "create group" in {
      forAll { (group: CompetenceGroup) =>
        val created = wait(dao.create(group))

        val groupFromDb = wait(dao.getById(created.id))
        groupFromDb mustBe defined
        created mustBe groupFromDb.get
      }
    }
  }

  "delete" should {
    "delete group" in {
      forAll { (group: CompetenceGroup) =>
        val created = wait(dao.create(group))

        wait(dao.delete(created.id))

        val groupFromDb = wait(dao.getById(created.id))
        groupFromDb mustBe empty
      }
    }
  }
  "update" should {
    "update group" in {
      val newGroupId = wait(dao.create(CompetenceGroups(0))).id

      forAll { (group: CompetenceGroup) =>
        val groupWithId = group.copy(id = newGroupId)

        wait(dao.update(groupWithId))

        val updatedFromDb = wait(dao.getById(newGroupId))

        updatedFromDb mustBe defined
        updatedFromDb.get mustBe groupWithId
      }
    }
  }
}
