package services

import models.ListWithTotal
import models.dao.{GroupDao, ProjectDao, ProjectRelationDao, UserGroupDao}
import models.group.Group
import models.project.{Project, Relation}
import org.davidbild.tristate.Tristate
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import testutils.fixture.{GroupFixture, ProjectFixture, UserFixture}
import testutils.generator.{GroupGenerator, TristateGenerator}
import utils.errors.{ConflictError, NotFoundError}
import utils.listmeta.ListMeta

/**
  * Test for group service.
  */
class GroupServiceTest extends BaseServiceTest with GroupGenerator with GroupFixture with TristateGenerator with ProjectFixture {

  private val admin = UserFixture.admin

  private case class TestFixture(
    groupDaoMock: GroupDao,
    userGroupDaoMock: UserGroupDao,
    projectDao: ProjectDao,
    relationDao: ProjectRelationDao,
    service: GroupService)

  private def getFixture = {
    val daoMock = mock[GroupDao]
    val userGroupDaoMock = mock[UserGroupDao]
    val projectDao = mock[ProjectDao]
    val relationDao = mock[ProjectRelationDao]
    val service = new GroupService(daoMock, userGroupDaoMock, relationDao, projectDao)
    TestFixture(daoMock, userGroupDaoMock, projectDao, relationDao, service)
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
      name: Option[String],
      groups: Seq[Group],
      total: Int
      ) =>
        val fixture = getFixture
        when(fixture.groupDaoMock.getList(
          optId = any[Option[Long]],
          optParentId = eqTo(parentId),
          optUserId = eqTo(userId),
          optName = eqTo(name),
          optLevels = any[Option[Seq[Int]]]
        )(eqTo(ListMeta.default)))
          .thenReturn(toFuture(ListWithTotal(total, groups)))
        val result = wait(fixture.service.list(parentId, userId, name, None)(admin, ListMeta.default).run)

        result mustBe 'right
        result.toOption.get mustBe ListWithTotal(total, groups)

        verify(fixture.groupDaoMock, times(1)).getList(
          optId = any[Option[Long]],
          optParentId = eqTo(parentId),
          optUserId = eqTo(userId),
          optName = eqTo(name),
          optLevels = any[Option[Seq[Int]]]
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

    "return conflict if can't delete" in {
      val fixture = getFixture
      val group = Groups(2)
      when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(Some(group)))
      when(fixture.projectDao.getList(
        optId = any[Option[Long]],
        optEventId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optFormId = any[Option[Long]],
        optGroupAuditorId = eqTo(Some(group.id)),
        optEmailTemplateId = any[Option[Long]],
        optAnyRelatedGroupId = any[Option[Long]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal(1, Projects.take(1))))
      when(fixture.relationDao.getList(
        optId = any[Option[Long]],
        optProjectId = any[Option[Long]],
        optKind = any[Option[Relation.Kind]],
        optFormId = any[Option[Long]],
        optGroupFromId = any[Option[Long]],
        optGroupToId = any[Option[Long]],
        optEmailTemplateId = any[Option[Long]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Relation](0, Nil)))

      val result = wait(fixture.service.delete(group.id)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "delete group from db" in {
      val fixture = getFixture
      val group = Groups(2)
      when(fixture.groupDaoMock.findById(group.id)).thenReturn(toFuture(Some(group)))
      when(fixture.projectDao.getList(
        optId = any[Option[Long]],
        optEventId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optFormId = any[Option[Long]],
        optGroupAuditorId = eqTo(Some(group.id)),
        optEmailTemplateId = any[Option[Long]],
        optAnyRelatedGroupId = any[Option[Long]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Project](0, Nil)))
      when(fixture.relationDao.getList(
        optId = any[Option[Long]],
        optProjectId = any[Option[Long]],
        optKind = any[Option[Relation.Kind]],
        optFormId = any[Option[Long]],
        optGroupFromId = any[Option[Long]],
        optGroupToId = any[Option[Long]],
        optEmailTemplateId = any[Option[Long]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Relation](0, Nil)))
      when(fixture.groupDaoMock.delete(group.id)).thenReturn(toFuture(1))
      val result = wait(fixture.service.delete(group.id)(admin).run)

      result mustBe 'right
    }
  }
}
