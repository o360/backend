package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import models.user.User
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.UserGroupService
import silhouette.DefaultEnv
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
    val controller = new UserGroupController(silhouette, userGroupService)
    TestFixture(silhouette, userGroupService, controller)
  }

  private val admin = User(1, None, None, User.Role.Admin, User.Status.Approved)

  "POST /groups/id/users/id" should {
    "return error if error happend" in {
      forAll { (groupId: Long, userId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userGroupServiceMock.add(groupId, userId)(admin))
          .thenReturn(toFuture(Left(NotFoundError.Group(groupId))))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.add(groupId, userId).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return 204 in success case" in {
      forAll { (groupId: Long, userId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userGroupServiceMock.add(groupId, userId)(admin))
          .thenReturn(toFuture(Right(())))

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
        when(fixture.userGroupServiceMock.remove(groupId, userId)(admin))
          .thenReturn(toFuture(Left(NotFoundError.Group(groupId))))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.remove(groupId, userId).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return 204 in success case" in {
      forAll { (groupId: Long, userId: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userGroupServiceMock.remove(groupId, userId)(admin))
          .thenReturn(toFuture(Right(())))

        val request = authenticated(FakeRequest(), env)
        val response = fixture.controller.remove(groupId, userId).apply(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }
}
