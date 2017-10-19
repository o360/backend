package controllers.admin

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.BaseControllerTest
import controllers.api.group.ApiUserGroup
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.UserGroupService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.TristateGenerator
import utils.errors.NotFoundError

/**
  * Test for user-group controller.
  */
class UserGroupControllerTest extends BaseControllerTest with TristateGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    userGroupServiceMock: UserGroupService,
    controller: UserGroupController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val userGroupService = mock[UserGroupService]
    val controller = new UserGroupController(silhouette, userGroupService, cc, ec)
    TestFixture(silhouette, userGroupService, controller)
  }

  private val admin = UserFixture.admin

  "POST /groups/id/users/id" should {
    "return error if error happend" in {
      forAll { (groupId: Long, userId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userGroupServiceMock.add(groupId, userId))
          .thenReturn(toErrorResult[Unit](NotFoundError.Group(groupId)))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.add(groupId, userId).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return 204 in success case" in {
      forAll { (groupId: Long, userId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userGroupServiceMock.add(groupId, userId)).thenReturn(toSuccessResult(()))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.add(groupId, userId).apply(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }

  "DELETE /groups/id/users/id" should {
    "return error if error happend" in {
      forAll { (groupId: Long, userId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userGroupServiceMock.remove(groupId, userId))
          .thenReturn(toErrorResult[Unit](NotFoundError.Group(groupId)))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.remove(groupId, userId).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return 204 in success case" in {
      forAll { (groupId: Long, userId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userGroupServiceMock.remove(groupId, userId)).thenReturn(toSuccessResult(()))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.remove(groupId, userId).apply(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }

  "POST /groups-users/add" should {
    "return error if error happend" in {
      forAll { groupsUsers: Seq[(Long, Long)] =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userGroupServiceMock.bulkAdd(groupsUsers))
          .thenReturn(toErrorResult[Unit](NotFoundError.Group(123)))

        val request = authenticated(
          FakeRequest("POST", "/groups-users/add")
            .withBody[Seq[ApiUserGroup]](groupsUsers.map(x => ApiUserGroup(x._1, x._2)))
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.bulkAdd.apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return 204 in success case" in {
      forAll { groupsUsers: Seq[(Long, Long)] =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userGroupServiceMock.bulkAdd(groupsUsers)).thenReturn(toSuccessResult(()))

        val request = authenticated(
          FakeRequest("POST", "/groups-users/add")
            .withBody[Seq[ApiUserGroup]](groupsUsers.map(x => ApiUserGroup(x._1, x._2)))
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.bulkAdd.apply(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }

  "POST /groups-users/remove" should {
    "return error if error happend" in {
      forAll { groupsUsers: Seq[(Long, Long)] =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userGroupServiceMock.bulkRemove(groupsUsers))
          .thenReturn(toErrorResult[Unit](NotFoundError.Group(123)))

        val request = authenticated(
          FakeRequest("POST", "/groups-users/remove")
            .withBody[Seq[ApiUserGroup]](groupsUsers.map(x => ApiUserGroup(x._1, x._2)))
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.bulkRemove.apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return 204 in success case" in {
      forAll { groupsUsers: Seq[(Long, Long)] =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userGroupServiceMock.bulkRemove(groupsUsers)).thenReturn(toSuccessResult(()))

        val request = authenticated(
          FakeRequest("POST", "/groups-users/remove")
            .withBody[Seq[ApiUserGroup]](groupsUsers.map(x => ApiUserGroup(x._1, x._2)))
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.bulkRemove.apply(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }
}
