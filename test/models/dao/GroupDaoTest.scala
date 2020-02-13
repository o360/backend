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

import models.group.{Group => GroupModel}
import org.davidbild.tristate.Tristate
import org.scalacheck.Gen
import testutils.fixture.{GroupFixture, UserGroupFixture}
import testutils.generator.{GroupGenerator, TristateGenerator}

/**
  * Test for group DAO.
  */
class GroupDaoTest
  extends BaseDaoTest
  with GroupFixture
  with GroupGenerator
  with TristateGenerator
  with UserGroupFixture {

  private val dao = inject[GroupDao]

  "get" should {
    "return groups by specific criteria" in {
      forAll(
        Gen.option(Gen.choose(-1L, 5L)),
        tristateArbitrary[Long].arbitrary
      ) {
        (
          id: Option[Long],
          parentId: Tristate[Long]
        ) =>
          val groups = wait(dao.getList(id, parentId))
          val expectedGroups =
            Groups.filter(u => id.forall(_ == u.id) & parentId.cata(u.parentId.contains(_), u.parentId.isEmpty, true))

          groups.total mustBe expectedGroups.length
          groups.data must contain theSameElementsAs expectedGroups
      }
    }

    "return groups filtered by user" in {
      forAll { (userId: Option[Long]) =>
        val groups = wait(dao.getList(optUserId = userId))
        val expectedGroups = Groups.filter(g =>
          userId match {
            case None      => true
            case Some(uid) => UserGroups.filter(_._1 == uid).map(_._2).contains(g.id)
          }
        )

        groups.data must contain theSameElementsAs expectedGroups
      }
    }

    "return groups filtered by name" in {
      val name = "-"
      val groups = wait(dao.getList(optName = Some(name))).data
      val expectedGroups = Groups.filter(_.name.toLowerCase.contains(name.toLowerCase))

      groups must contain theSameElementsAs expectedGroups
    }
  }

  "findById" should {
    "return group by id" in {
      forAll(Gen.choose(-1L, 5L)) { (id: Long) =>
        val group = wait(dao.findById(id))

        group mustBe Groups.find(_.id == id)
      }
    }
  }

  "create" should {
    "create group" in {
      forAll(groupArbitrary.arbitrary, Gen.option(Gen.choose(-1L, 5L))) { (group: GroupModel, parentId: Option[Long]) =>
        val g =
          group.copy(parentId = parentId, hasChildren = false, name = java.util.UUID.randomUUID.toString, level = 0)
        whenever(g.parentId.isEmpty || wait(dao.findById(g.parentId.get)).nonEmpty) {
          val createdGroup = wait(dao.create(g))
          val groupById = wait(dao.findById(createdGroup.id))

          groupById mustBe defined
          groupById.get mustBe createdGroup
        }
      }
    }
  }

  "update" should {
    "update group" in {
      val newGroupId = wait(dao.create(Groups(0).copy(name = java.util.UUID.randomUUID.toString))).id
      forAll(groupArbitrary.arbitrary, Gen.option(Gen.choose(-1L, 5L))) { (group: GroupModel, parentId: Option[Long]) =>
        val g = group.copy(
          id = newGroupId,
          parentId = parentId,
          name = java.util.UUID.randomUUID.toString,
          hasChildren = false,
          level = 0
        )
        whenever(g.parentId.isEmpty || wait(dao.findById(g.parentId.get)).nonEmpty) {
          wait(dao.update(g))
          val updatedGroup = wait(dao.findById(newGroupId))

          updatedGroup mustBe Some(g)
        }
      }
    }
  }

  "delete" should {
    "delete group" in {
      forAll { (group: GroupModel) =>
        val newGroupId = wait(dao.create(group.copy(parentId = None))).id
        wait(dao.delete(newGroupId))
        val deletedGroup = wait(dao.findById(newGroupId))

        deletedGroup mustBe empty
      }
    }
  }

  "findChildrenIds" should {
    "return children ids" in {
      val children = wait(dao.findChildrenIds(2))

      children must contain theSameElementsAs Seq(3, 4, 5)
    }

    "return Nil if there are no children" in {
      val children = wait(dao.findChildrenIds(1))

      children mustBe empty
    }
  }

  "findGroupIdsByUserId" should {
    "return group ids" in {
      val groupIds = wait(dao.findGroupIdsByUserId(2))

      groupIds must contain theSameElementsAs Seq(2, 3, 4, 5)
    }

    "return Nil if there are no groups" in {
      val groupIds = wait(dao.findGroupIdsByUserId(3))

      groupIds mustBe empty
    }
  }
}
