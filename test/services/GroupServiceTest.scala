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

package services

import models.ListWithTotal
import models.dao.{GroupDao, ProjectDao, ProjectRelationDao, UserGroupDao}
import models.group.Group
import models.project.{Project, Relation}
import org.davidbild.tristate.Tristate
import org.mockito.Mockito._
import testutils.fixture.{GroupFixture, ProjectFixture}
import testutils.generator.{GroupGenerator, TristateGenerator}
import utils.errors.{ConflictError, NotFoundError}
import utils.listmeta.ListMeta

/**
  * Test for group service.
  */
class GroupServiceTest
  extends BaseServiceTest
  with GroupGenerator
  with GroupFixture
  with TristateGenerator
  with ProjectFixture {

  private case class TestFixture(
    groupDaoMock: GroupDao,
    userGroupDaoMock: UserGroupDao,
    projectDao: ProjectDao,
    relationDao: ProjectRelationDao,
    service: GroupService
  )

  private def getFixture = {
    val daoMock = mock[GroupDao]
    val userGroupDaoMock = mock[UserGroupDao]
    val projectDao = mock[ProjectDao]
    val relationDao = mock[ProjectRelationDao]
    val service = new GroupService(daoMock, userGroupDaoMock, relationDao, projectDao, ec)
    TestFixture(daoMock, userGroupDaoMock, projectDao, relationDao, service)
  }

  "getById" should {

    "return not found if group not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.groupDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.getById(id).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.groupDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.groupDaoMock)
      }
    }

    "return group from db" in {
      forAll { (group: Group, id: Long) =>
        val fixture = getFixture
        when(fixture.groupDaoMock.findById(id)).thenReturn(toFuture(Some(group)))
        val result = wait(fixture.service.getById(id).run)

        result mustBe right
        result.toOption.get mustBe group

        verify(fixture.groupDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.groupDaoMock)
      }
    }
  }

  "list" should {
    "return list of groups from db" in {
      forAll {
        (
          parentId: Tristate[Long],
          userId: Option[Long],
          name: Option[String],
          groups: Seq[Group],
          total: Int
        ) =>
          val fixture = getFixture
          when(
            fixture.groupDaoMock.getList(
              optId = *,
              optParentId = eqTo(parentId),
              optUserId = eqTo(userId),
              optName = eqTo(name),
              optLevels = *
            )(eqTo(ListMeta.default))
          ).thenReturn(toFuture(ListWithTotal(total, groups)))
          val result = wait(fixture.service.list(parentId, userId, name, None)(ListMeta.default).run)

          result mustBe right
          result.toOption.get mustBe ListWithTotal(total, groups)

          verify(fixture.groupDaoMock, times(1)).getList(
            optId = *,
            optParentId = eqTo(parentId),
            optUserId = eqTo(userId),
            optName = eqTo(name),
            optLevels = *
          )(eqTo(ListMeta.default))
      }
    }
  }

  "listByUserId" should {
    "return all groups for user ID" in {
      forAll { (userId: Long, group: Group) =>
        val fixture = getFixture
        when(
          fixture.groupDaoMock.getList(
            optId = *,
            optParentId = *,
            optUserId = eqTo(Some(userId)),
            optName = *,
            optLevels = *
          )(*)
        ).thenReturn(toFuture(ListWithTotal(Seq(group))))
        group.parentId.foreach { parentId =>
          when(fixture.groupDaoMock.findById(parentId)).thenReturn(toFuture(Some(group.copy(parentId = None))))
        }

        val result = wait(fixture.service.listByUserId(userId).run)

        result mustBe right
        result.toOption.get.data mustBe group.parentId.map(_ => group.copy(parentId = None)) ++ Seq(group)
      }
    }
  }

  "create" should {
    "return not found if parent not found" in {
      forAll { (group: Group) =>
        whenever(group.parentId.nonEmpty && group.parentId.get != group.id) {
          val fixture = getFixture
          when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(Some(group)))
          when(fixture.groupDaoMock.findById(group.parentId.get)).thenReturn(toFuture(None))
          val result = wait(fixture.service.create(group).run)

          result mustBe left
          result.swap.toOption.get mustBe a[NotFoundError]
        }
      }
    }

    "create group in db" in {
      val fixture = getFixture
      val group = Groups(2)
      when(fixture.groupDaoMock.findById(group.parentId.get)).thenReturn(toFuture(Some(group)))
      when(fixture.groupDaoMock.create(group.copy(id = 0))).thenReturn(toFuture(group))
      val result = wait(fixture.service.create(group.copy(id = 0)).run)

      result mustBe right
      result.toOption.get mustBe group
    }
  }

  "update" should {
    "return not found if group not found" in {
      forAll { (group: Group) =>
        val fixture = getFixture
        when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.update(group).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.groupDaoMock, times(1)).findById(group.id)
        verifyNoMoreInteractions(fixture.groupDaoMock)
      }
    }

    "return conflict if group is parent to itself" in {
      forAll { (gr: Group) =>
        whenever(gr.id != 0) {
          val group = gr.copy(parentId = Some(gr.id))
          val fixture = getFixture
          when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(Some(group)))
          val result = wait(fixture.service.update(group).run)

          result mustBe left
          result.swap.toOption.get mustBe a[ConflictError]

          verify(fixture.groupDaoMock, times(1)).findById(group.id)
          verifyNoMoreInteractions(fixture.groupDaoMock)
        }
      }
    }

    "return not found if parent not found" in {
      forAll { (group: Group) =>
        whenever(group.parentId.nonEmpty && group.parentId.get != group.id) {
          val fixture = getFixture
          when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(Some(group)))
          when(fixture.groupDaoMock.findById(group.parentId.get)).thenReturn(toFuture(None))
          val result = wait(fixture.service.update(group).run)

          result mustBe left
          result.swap.toOption.get mustBe a[NotFoundError]
        }
      }
    }

    "return conflict if circular parent-child reference" in {
      val fixture = getFixture
      val group = Groups(2)
      when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(Some(group)))
      when(fixture.groupDaoMock.findById(group.parentId.get)).thenReturn(toFuture(Some(group)))
      when(fixture.groupDaoMock.findChildrenIds(group.id)).thenReturn(toFuture(Seq(group.parentId.get)))
      val result = wait(fixture.service.update(group).run)

      result mustBe left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "update group in db" in {
      val fixture = getFixture
      val group = Groups(2)
      when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(Some(group)))
      when(fixture.groupDaoMock.findById(group.parentId.get)).thenReturn(toFuture(Some(group)))
      when(fixture.groupDaoMock.findChildrenIds(group.id)).thenReturn(toFuture(Nil))
      when(fixture.groupDaoMock.update(group)).thenReturn(toFuture(group))
      val result = wait(fixture.service.update(group).run)

      result mustBe right
      result.toOption.get mustBe group
    }
  }

  "delete" should {
    "return not found if group not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.groupDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.delete(id).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.groupDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.groupDaoMock)
      }
    }

    "return conflict if can't delete" in {
      val fixture = getFixture
      val group = Groups(2)
      when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(Some(group)))
      when(fixture.groupDaoMock.findChildrenIds(group.id)).thenReturn(toFuture(Nil))
      when(
        fixture.projectDao.getList(
          optId = *,
          optEventId = *,
          optGroupFromIds = *,
          optFormId = *,
          optGroupAuditorId = eqTo(Some(group.id)),
          optEmailTemplateId = *,
          optAnyRelatedGroupId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal(1, Projects.take(1))))
      when(
        fixture.relationDao.getList(
          optId = *,
          optProjectId = *,
          optKind = *,
          optFormId = *,
          optGroupFromId = *,
          optGroupToId = *,
          optEmailTemplateId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal[Relation](0, Nil)))

      val result = wait(fixture.service.delete(group.id).run)

      result mustBe left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "delete group from db" in {
      val fixture = getFixture
      val group = Groups(2)
      when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(Some(group)))
      when(fixture.groupDaoMock.findChildrenIds(group.id)).thenReturn(toFuture(Nil))
      when(
        fixture.projectDao.getList(
          optId = *,
          optEventId = *,
          optGroupFromIds = *,
          optFormId = *,
          optGroupAuditorId = eqTo(Some(group.id)),
          optEmailTemplateId = *,
          optAnyRelatedGroupId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal[Project](0, Nil)))
      when(
        fixture.relationDao.getList(
          optId = *,
          optProjectId = *,
          optKind = *,
          optFormId = *,
          optGroupFromId = *,
          optGroupToId = *,
          optEmailTemplateId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal[Relation](0, Nil)))
      when(fixture.groupDaoMock.delete(group.id)).thenReturn(toFuture(1))
      val result = wait(fixture.service.delete(group.id).run)

      result mustBe right
    }
  }
}
