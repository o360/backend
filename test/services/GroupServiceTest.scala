package services

import models.ListWithTotal
import models.dao.{GroupDao, UserGroupDao}
import models.group.Group
import org.davidbild.tristate.Tristate
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import testutils.fixture.{GroupFixture, UserFixture}
import testutils.generator.{GroupGenerator, TristateGenerator}
import utils.errors.{ConflictError, NotFoundError}
import utils.listmeta.ListMeta

/**
  * Test for group service.
  */
class GroupServiceTest extends BaseServiceTest with GroupGenerator with GroupFixture with TristateGenerator {

  private val admin = UserFixture.admin

  private case class TestFixture(
    groupDaoMock: GroupDao,
    userGroupDaoMock: UserGroupDao,
    service: GroupService)

  private def getFixture = {
    val daoMock = mock[GroupDao]
    val userGroupDaoMock = mock[UserGroupDao]
    val service = new GroupService(daoMock, userGroupDaoMock)
    TestFixture(daoMock, userGroupDaoMock, service)
  }

  "getById" should {

    "return not found if group not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.groupDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.getById(id)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.groupDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.groupDaoMock)
      }
    }

    "return group from db" in {
      forAll { (group: Group, id: Long) =>
        val fixture = getFixture
        when(fixture.groupDaoMock.findById(id)).thenReturn(toFuture(Some(group)))
        val result = wait(fixture.service.getById(id)(admin).run)

        result mustBe 'right
        result.toOption.get mustBe group

        verify(fixture.groupDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.groupDaoMock)
      }
    }
  }

  "list" should {
    "return list of groups from db" in {
      forAll { (
      parentId: Tristate[Long],
      userId: Option[Long],
      groups: Seq[Group],
      total: Int
      ) =>
        val fixture = getFixture
        when(fixture.groupDaoMock.getList(
          optId = any[Option[Long]],
          optParentId = eqTo(parentId),
          optUserId = eqTo(userId)
        )(eqTo(ListMeta.default)))
          .thenReturn(toFuture(ListWithTotal(total, groups)))
        val result = wait(fixture.service.list(parentId, userId)(admin, ListMeta.default).run)

        result mustBe 'right
        result.toOption.get mustBe ListWithTotal(total, groups)

        verify(fixture.groupDaoMock, times(1)).getList(
          optId = any[Option[Long]],
          optParentId = eqTo(parentId),
          optUserId = eqTo(userId)
        )(eqTo(ListMeta.default))
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
          val result = wait(fixture.service.create(group)(admin).run)

          result mustBe 'left
          result.swap.toOption.get mustBe a[NotFoundError]
        }
      }
    }

    "create group in db" in {
      val fixture = getFixture
      val group = Groups(2)
      when(fixture.groupDaoMock.findById(group.parentId.get)).thenReturn(toFuture(Some(group)))
      when(fixture.groupDaoMock.create(group.copy(id = 0))).thenReturn(toFuture(group))
      val result = wait(fixture.service.create(group.copy(id = 0))(admin).run)

      result mustBe 'right
      result.toOption.get mustBe group
    }
  }

  "update" should {
    "return not found if group not found" in {
      forAll { (group: Group) =>
        val fixture = getFixture
        when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.update(group)(admin).run)

        result mustBe 'left
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
          val result = wait(fixture.service.update(group)(admin).run)

          result mustBe 'left
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
          val result = wait(fixture.service.update(group)(admin).run)

          result mustBe 'left
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
      val result = wait(fixture.service.update(group)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "update group in db" in {
      val fixture = getFixture
      val group = Groups(2)
      when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(Some(group)))
      when(fixture.groupDaoMock.findById(group.parentId.get)).thenReturn(toFuture(Some(group)))
      when(fixture.groupDaoMock.findChildrenIds(group.id)).thenReturn(toFuture(Nil))
      when(fixture.groupDaoMock.update(group)).thenReturn(toFuture(group))
      val result = wait(fixture.service.update(group)(admin).run)

      result mustBe 'right
      result.toOption.get mustBe group
    }
  }

  "delete" should {
    "return not found if group not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.groupDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.delete(id)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.groupDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.groupDaoMock)
      }
    }

    "return conflict if children exists" in {
      val fixture = getFixture
      val group = Groups(2)
      when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(Some(group)))
      when(fixture.groupDaoMock.findChildrenIds(group.id)).thenReturn(toFuture(Seq(1L, 2L, 3L, 4L)))
      val result = wait(fixture.service.delete(group.id)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "return conflict if users in group exists" in {
      val fixture = getFixture
      val group = Groups(2)
      when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(Some(group)))
      when(fixture.groupDaoMock.findChildrenIds(group.id)).thenReturn(toFuture(Nil))
      when(fixture.userGroupDaoMock.exists(groupId = eqTo(Some(group.id)), userId = any[Option[Long]]))
        .thenReturn(toFuture(true))
      val result = wait(fixture.service.delete(group.id)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "delete group from db" in {
      val fixture = getFixture
      val group = Groups(2)
      when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(Some(group)))
      when(fixture.groupDaoMock.findChildrenIds(group.id)).thenReturn(toFuture(Nil))
      when(fixture.groupDaoMock.delete(group.id)).thenReturn(toFuture(1))
      when(fixture.userGroupDaoMock.exists(groupId = eqTo(Some(group.id)), userId = any[Option[Long]])).thenReturn(toFuture(false))
      val result = wait(fixture.service.delete(group.id)(admin).run)

      result mustBe 'right
    }
  }
}
