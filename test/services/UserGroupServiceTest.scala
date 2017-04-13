package services

import models.dao.UserGroupDao
import models.user.{User => UserModel}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import testutils.fixture.UserGroupFixture
import testutils.generator.TristateGenerator
import utils.errors.NotFoundError

/**
  * Test for user-group service.
  */
class UserGroupServiceTest extends BaseServiceTest with TristateGenerator with UserGroupFixture {

  private val admin = UserModel(1, None, None, UserModel.Role.Admin, UserModel.Status.Approved)

  private case class TestFixture(
    userGroupDaoMock: UserGroupDao,
    userServiceMock: UserService,
    groupServiceMock: GroupService,
    service: UserGroupService)

  private def getFixture = {
    val userGroupDao = mock[UserGroupDao]
    val userService = mock[UserService]
    val groupService = mock[GroupService]
    val service = new UserGroupService(userService, groupService, userGroupDao)
    TestFixture(userGroupDao, userService, groupService, service)
  }

  "add" should {
    "return error if user not found" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)(admin)).thenReturn(toFuture(Left(NotFoundError.User(userId))))
        val result = wait(fixture.service.add(groupId, userId)(admin))

        result mustBe 'left
        result.left.get mustBe a[NotFoundError]
      }
    }

    "return error if group not found" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)(admin)).thenReturn(toFuture(Right(admin)))
        when(fixture.groupServiceMock.getById(groupId)(admin)).thenReturn(toFuture(Left(NotFoundError.Group(groupId))))
        val result = wait(fixture.service.add(groupId, userId)(admin))

        result mustBe 'left
        result.left.get mustBe a[NotFoundError]
      }
    }

    "not add if user already in group" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)(admin)).thenReturn(toFuture(Right(admin)))
        when(fixture.groupServiceMock.getById(groupId)(admin)).thenReturn(toFuture(Right(Groups(0))))
        when(fixture.userGroupDaoMock.exists(groupId = eqTo(Some(groupId)), userId = eqTo(Some(userId))))
          .thenReturn(toFuture(true))
        val result = wait(fixture.service.add(groupId, userId)(admin))

        result mustBe 'right
        verify(fixture.userGroupDaoMock, times(1)).exists(groupId = Some(groupId), userId = Some(userId))
      }
    }

    "add user to group" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)(admin)).thenReturn(toFuture(Right(admin)))
        when(fixture.groupServiceMock.getById(groupId)(admin)).thenReturn(toFuture(Right(Groups(0))))
        when(fixture.userGroupDaoMock.exists(groupId = eqTo(Some(groupId)), userId = eqTo(Some(userId))))
          .thenReturn(toFuture(false))
        when(fixture.userGroupDaoMock.add(groupId, userId)).thenReturn(toFuture(()))
        val result = wait(fixture.service.add(groupId, userId)(admin))

        result mustBe 'right
        verify(fixture.userGroupDaoMock, times(1)).exists(groupId = Some(groupId), userId = Some(userId))
        verify(fixture.userGroupDaoMock, times(1)).add(groupId, userId)
      }
    }
  }

  "remove" should {
    "return error if user not found" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)(admin)).thenReturn(toFuture(Left(NotFoundError.User(userId))))
        val result = wait(fixture.service.remove(groupId, userId)(admin))

        result mustBe 'left
        result.left.get mustBe a[NotFoundError]
      }
    }

    "return error if group not found" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)(admin)).thenReturn(toFuture(Right(admin)))
        when(fixture.groupServiceMock.getById(groupId)(admin)).thenReturn(toFuture(Left(NotFoundError.Group(groupId))))
        val result = wait(fixture.service.remove(groupId, userId)(admin))

        result mustBe 'left
        result.left.get mustBe a[NotFoundError]
      }
    }

    "remove user from group" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)(admin)).thenReturn(toFuture(Right(admin)))
        when(fixture.groupServiceMock.getById(groupId)(admin)).thenReturn(toFuture(Right(Groups(0))))
        when(fixture.userGroupDaoMock.remove(groupId, userId)).thenReturn(toFuture(()))
        val result = wait(fixture.service.remove(groupId, userId)(admin))

        result mustBe 'right
        verify(fixture.userGroupDaoMock, times(1)).remove(groupId, userId)
      }
    }
  }
}

