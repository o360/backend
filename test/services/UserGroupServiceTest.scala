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

import models.dao.UserGroupDao
import models.group.Group
import models.user.User
import org.mockito.Mockito._
import testutils.fixture.{UserFixture, UserGroupFixture}
import testutils.generator.TristateGenerator
import utils.errors.{ConflictError, NotFoundError}

/**
  * Test for user-group service.
  */
class UserGroupServiceTest extends BaseServiceTest with TristateGenerator with UserGroupFixture {

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
        when(fixture.userServiceMock.getById(userId)).thenReturn(toErrorResult[User](NotFoundError.User(userId)))
        val result = wait(fixture.service.add(groupId, userId).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return error if group not found" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)).thenReturn(toSuccessResult(admin))
        when(fixture.groupServiceMock.getById(groupId)).thenReturn(toErrorResult[Group](NotFoundError.Group(groupId)))
        val result = wait(fixture.service.add(groupId, userId).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return error if user is unapproved" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)).thenReturn(toSuccessResult(admin.copy(status = User.Status.New)))
        when(fixture.groupServiceMock.getById(groupId)).thenReturn(toSuccessResult(Groups(0)))

        val result = wait(fixture.service.add(groupId, userId).run)

        result mustBe left
        result.swap.toOption.get mustBe a[ConflictError]
      }
    }

    "not add if user already in group" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)).thenReturn(toSuccessResult(admin))
        when(fixture.groupServiceMock.getById(groupId)).thenReturn(toSuccessResult(Groups(0)))
        when(fixture.userGroupDaoMock.exists(groupId = eqTo(Some(groupId)), userId = eqTo(Some(userId))))
          .thenReturn(toFuture(true))
        val result = wait(fixture.service.add(groupId, userId).run)

        result mustBe right
        verify(fixture.userGroupDaoMock, times(1)).exists(groupId = Some(groupId), userId = Some(userId))
      }
    }

    "add user to group" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)).thenReturn(toSuccessResult(admin))
        when(fixture.groupServiceMock.getById(groupId)).thenReturn(toSuccessResult(Groups(0)))
        when(fixture.userGroupDaoMock.exists(groupId = eqTo(Some(groupId)), userId = eqTo(Some(userId))))
          .thenReturn(toFuture(false))
        when(fixture.userGroupDaoMock.add(groupId, userId)).thenReturn(toFuture(()))
        val result = wait(fixture.service.add(groupId, userId).run)

        result mustBe right
        verify(fixture.userGroupDaoMock, times(1)).exists(groupId = Some(groupId), userId = Some(userId))
        verify(fixture.userGroupDaoMock, times(1)).add(groupId, userId)
      }
    }
  }

  "remove" should {
    "return error if user not found" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)).thenReturn(toErrorResult[User](NotFoundError.User(userId)))
        val result = wait(fixture.service.remove(groupId, userId).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return error if group not found" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)).thenReturn(toSuccessResult(admin))
        when(fixture.groupServiceMock.getById(groupId)).thenReturn(toErrorResult[Group](NotFoundError.Group(groupId)))
        val result = wait(fixture.service.remove(groupId, userId).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]
      }
    }

    "return error if user is unapproved" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)).thenReturn(toSuccessResult(admin.copy(status = User.Status.New)))
        when(fixture.groupServiceMock.getById(groupId)).thenReturn(toSuccessResult(Groups(0)))

        val result = wait(fixture.service.remove(groupId, userId).run)

        result mustBe left
        result.swap.toOption.get mustBe a[ConflictError]
      }
    }

    "remove user from group" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)).thenReturn(toSuccessResult(admin))
        when(fixture.groupServiceMock.getById(groupId)).thenReturn(toSuccessResult(Groups(0)))
        when(fixture.userGroupDaoMock.remove(groupId, userId)).thenReturn(toFuture(()))
        val result = wait(fixture.service.remove(groupId, userId).run)

        result mustBe right
        verify(fixture.userGroupDaoMock, times(1)).remove(groupId, userId)
      }
    }
  }

  "bulkAdd" should {
    "bulk add users to groups" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)).thenReturn(toSuccessResult(admin))
        when(fixture.groupServiceMock.getById(groupId)).thenReturn(toSuccessResult(Groups(0)))
        when(fixture.userGroupDaoMock.exists(groupId = eqTo(Some(groupId)), userId = eqTo(Some(userId))))
          .thenReturn(toFuture(false))
        when(fixture.userGroupDaoMock.add(groupId, userId)).thenReturn(toFuture(()))
        val result = wait(fixture.service.bulkAdd(Seq((groupId, userId))).run)

        result mustBe right
        verify(fixture.userGroupDaoMock, times(1)).exists(groupId = Some(groupId), userId = Some(userId))
        verify(fixture.userGroupDaoMock, times(1)).add(groupId, userId)
      }
    }
  }

  "bulkRemove" should {
    "bulk remove users from groups" in {
      forAll { (groupId: Long, userId: Long) =>
        val fixture = getFixture
        when(fixture.userServiceMock.getById(userId)).thenReturn(toSuccessResult(admin))
        when(fixture.groupServiceMock.getById(groupId)).thenReturn(toSuccessResult(Groups(0)))
        when(fixture.userGroupDaoMock.remove(groupId, userId)).thenReturn(toFuture(()))
        val result = wait(fixture.service.bulkRemove(Seq((groupId, userId))).run)

        result mustBe right
        verify(fixture.userGroupDaoMock, times(1)).remove(groupId, userId)
      }
    }
  }
}
