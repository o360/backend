package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.api.Response
import controllers.api.user.ApiUser
import models.ListWithTotal
import models.user.{User => UserModel}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.UserService
import silhouette.DefaultEnv
import testutils.generator.UserGenerator
import utils.errors.NotFoundError
import utils.listmeta.ListMeta

/**
  * Test for user controller.
  */
class UserControllerTest extends BaseControllerTest with UserGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    userServiceMock: UserService,
    controller: UserController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val userServiceMock = mock[UserService]
    val controller = new UserController(silhouette, userServiceMock)
    TestFixture(silhouette, userServiceMock, controller)
  }

  private val admin = UserModel(1, None, None, UserModel.Role.Admin, UserModel.Status.Approved)

  "GET /users/id" should {
    "return not found if user not found" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userServiceMock.getById(id)(admin)).thenReturn(toFuture(Left(NotFoundError.User(id))))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return json with user" in {
      forAll { (id: Long, user: UserModel) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userServiceMock.getById(id)(admin)).thenReturn(toFuture(Right(user)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id)(request)
        status(response) mustBe OK
        val userJson = contentAsJson(response)
        userJson mustBe Json.toJson(ApiUser(user))
      }
    }
  }

  "GET /users" should {
    "return users list from service" in {
      forAll { (
      role: Option[UserModel.Role],
      st: Option[UserModel.Status],
      total: Int,
      users: Seq[UserModel]
      ) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userServiceMock.list(role, st)(admin, ListMeta.default))
          .thenReturn(toFuture(Right(ListWithTotal(total, users))))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller
          .getList(role.map(ApiUser.ApiRole(_)), st.map(ApiUser.ApiStatus(_)))(request)

        status(response) mustBe OK
        val usersJson = contentAsJson(response)
        val expectedJson = Json.toJson(
          Response.List(Response.Meta(total, ListMeta.default), users.map(ApiUser(_)))
        )
        usersJson mustBe expectedJson
      }
    }
    "return forbidden for non admin user" in {
      val env = fakeEnvironment(admin.copy(role = UserModel.Role.User))
      val fixture = getFixture(env)
      val request = authenticated(FakeRequest(), env)
      val response = fixture.controller.getList(None, None).apply(request)

      status(response) mustBe FORBIDDEN
    }
  }

  "PUT /users" should {
    "update users" in {
      forAll { (id: Long, user: UserModel) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        val userWithId = user.copy(id = id)
        when(fixture.userServiceMock.update(userWithId)(admin))
          .thenReturn(toFuture(Right(userWithId)))
        val request = authenticated(
          FakeRequest("POST", "/users")
            .withBody[ApiUser](ApiUser(user))
            .withHeaders(CONTENT_TYPE -> "application/json"),
          env
        )

        val response = fixture.controller.update(id).apply(request)
        val responseJson = contentAsJson(response)
        val expectedJson = Json.toJson(ApiUser(userWithId))

        status(response) mustBe OK
        responseJson mustBe expectedJson
      }
    }
  }

  "DELETE /users" should {
    "delete users" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(admin)
        val fixture = getFixture(env)
        when(fixture.userServiceMock.delete(id)(admin)).thenReturn(toFuture(Right(())))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.delete(id)(request)
        status(response) mustBe NO_CONTENT
      }
    }
  }
}
