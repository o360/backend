package services

import models.dao.UserGroupDao
import models.group.Group
import models.user.User
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import testutils.fixture.{UserFixture, UserGroupFixture}
import testutils.generator.TristateGenerator
import utils.errors.{ApplicationError, ConflictError, NotFoundError}

import scalaz.{-\/, \/, \/-, EitherT}

/**
  * Test for user-group service.
  */
class UserGroupServiceTest extends BaseServiceTest with TristateGenerator with UserGroupFixture with MockitoSugar {

  private val admin = UserFixture.admin

  private case class TestFixture(
    userGroupDaoMock: UserGroupDao,
    userServiceMock: UserService,
    groupServiceMock: GroupService,
    service: UserGroupService
  )

  private def getFixture = {
    val userGroupDao = mock[UserGroupDao]
    val userService = mock[UserService]
    val groupService = mock[GroupService]
    val service = new UserGroupService(userService, groupService, userGroupDao, ec)
    TestFixture(userGroupDao, userService, groupService, service)
  }

  "add" should {
    "return error if user not found" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId))
          .thenReturn(EitherT.eitherT(toFuture(-\/(NotFoundError.User(userId)): ApplicationError \/ User)))
        val result = wait(fixture.service.add(groupId, userId).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return error if group not found" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(admin): ApplicationError \/ User)))
        when(fixture.groupServiceMock.getById(groupId))
          .thenReturn(EitherT.eitherT(toFuture(-\/(NotFoundError.Group(groupId)): ApplicationError \/ Group)))
        val result = wait(fixture.service.add(groupId, userId).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return error if user is unapproved" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(admin.copy(status = User.Status.New)): ApplicationError \/ User)))
        when(fixture.groupServiceMock.getById(groupId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Groups(0)): ApplicationError \/ Group)))

        val result = wait(fixture.service.add(groupId, userId).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[ConflictError]
      }
    }

    "not add if user already in group" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(admin): ApplicationError \/ User)))
        when(fixture.groupServiceMock.getById(groupId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Groups(0)): ApplicationError \/ Group)))

        when(fixture.userGroupDaoMock.exists(groupId = eqTo(Some(groupId)), userId = eqTo(Some(userId))))
          .thenReturn(toFuture(true))
        val result = wait(fixture.service.add(groupId, userId).run)

        result mustBe 'right
        verify(fixture.userGroupDaoMock, times(1)).exists(groupId = Some(groupId), userId = Some(userId))
      }
    }

    "add user to group" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(admin): ApplicationError \/ User)))
        when(fixture.groupServiceMock.getById(groupId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Groups(0)): ApplicationError \/ Group)))
        when(fixture.userGroupDaoMock.exists(groupId = eqTo(Some(groupId)), userId = eqTo(Some(userId))))
          .thenReturn(toFuture(false))
        when(fixture.userGroupDaoMock.add(groupId, userId)).thenReturn(toFuture(()))
        val result = wait(fixture.service.add(groupId, userId).run)

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
        when(fixture.userServiceMock.getById(userId))
          .thenReturn(EitherT.eitherT(toFuture(-\/(NotFoundError.User(userId)): ApplicationError \/ User)))
        val result = wait(fixture.service.remove(groupId, userId).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return error if group not found" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(admin): ApplicationError \/ User)))
        when(fixture.groupServiceMock.getById(groupId))
          .thenReturn(EitherT.eitherT(toFuture(-\/(NotFoundError.Group(groupId)): ApplicationError \/ Group)))
        val result = wait(fixture.service.remove(groupId, userId).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return error if user is unapproved" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(admin.copy(status = User.Status.New)): ApplicationError \/ User)))
        when(fixture.groupServiceMock.getById(groupId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Groups(0)): ApplicationError \/ Group)))

        val result = wait(fixture.service.remove(groupId, userId).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[ConflictError]
      }
    }

    "remove user from group" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(admin): ApplicationError \/ User)))
        when(fixture.groupServiceMock.getById(groupId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Groups(0)): ApplicationError \/ Group)))
        when(fixture.userGroupDaoMock.remove(groupId, userId)).thenReturn(toFuture(()))
        val result = wait(fixture.service.remove(groupId, userId).run)

        result mustBe 'right
        verify(fixture.userGroupDaoMock, times(1)).remove(groupId, userId)
      }
    }
  }

  "bulkAdd" should {
    "bulk add users to groups" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(admin): ApplicationError \/ User)))
        when(fixture.groupServiceMock.getById(groupId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Groups(0)): ApplicationError \/ Group)))
        when(fixture.userGroupDaoMock.exists(groupId = eqTo(Some(groupId)), userId = eqTo(Some(userId))))
          .thenReturn(toFuture(false))
        when(fixture.userGroupDaoMock.add(groupId, userId)).thenReturn(toFuture(()))
        val result = wait(fixture.service.bulkAdd(Seq((groupId, userId))).run)

        result mustBe 'right
        verify(fixture.userGroupDaoMock, times(1)).exists(groupId = Some(groupId), userId = Some(userId))
        verify(fixture.userGroupDaoMock, times(1)).add(groupId, userId)
      }
    }
  }

  "bulkRemove" should {
    "bulk remove users from groups" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(admin): ApplicationError \/ User)))
        when(fixture.groupServiceMock.getById(groupId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(Groups(0)): ApplicationError \/ Group)))
        when(fixture.userGroupDaoMock.remove(groupId, userId)).thenReturn(toFuture(()))
        val result = wait(fixture.service.bulkRemove(Seq((groupId, userId))).run)

        result mustBe 'right
        verify(fixture.userGroupDaoMock, times(1)).remove(groupId, userId)
      }
    }
  }
}
