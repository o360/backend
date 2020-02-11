package controllers.user

import com.mohiva.play.silhouette.api.{LoginInfo, Silhouette}
import com.mohiva.play.silhouette.test._
import controllers.BaseControllerTest
import controllers.api.Response
import controllers.api.user.{ApiPartialUser, ApiShortUser, ApiUser}
import models.ListWithTotal
import models.user.{User, UserShort}
import org.davidbild.tristate.Tristate
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import services.UserService
import silhouette.DefaultEnv
import testutils.fixture.UserFixture
import testutils.generator.UserGenerator
import utils.errors.NotFoundError
import utils.listmeta.ListMeta
import io.scalaland.chimney.dsl._

import scala.concurrent.ExecutionContext

/**
  * Authentication controller test.
  */
class UserControllerTest extends BaseControllerTest with UserGenerator {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    userService: UserService,
    controller: UserController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val userServiceMock = mock[UserService]
    val controller = new UserController(silhouette, userServiceMock, cc, ExecutionContext.Implicits.global)
    TestFixture(silhouette, userServiceMock, controller)
  }

  "GET /users/current" should {
    "return user's json if user is present" in {
      forAll { (user: User, provId: String, provKey: String) =>
        implicit val env = FakeEnvironment[DefaultEnv](Seq(LoginInfo(provId, provKey) -> user))

        val fixture = getFixture(env)
        val request = FakeRequest().withAuthenticator(LoginInfo(provId, provKey))

        val result = fixture.controller.me(request)
        status(result) mustBe OK
        val userJson = contentAsJson(result)
        userJson mustBe Json.toJson(ApiUser(user))
      }
    }
    "return unauthorized if user is not present" in {
      forAll { (provId: String, provKey: String) =>
        implicit val env = FakeEnvironment[DefaultEnv](Nil)
        val fixture = getFixture(env)
        val request = FakeRequest().withAuthenticator(LoginInfo(provId, provKey))

        val result = fixture.controller.me(request)
        status(result) mustBe UNAUTHORIZED
      }
    }

    "return unauthorized if authenticator is not present" in {
      forAll { (user: User, provId: String, provKey: String) =>
        implicit val env = FakeEnvironment[DefaultEnv](Seq(LoginInfo(provId, provKey) -> user))
        val fixture = getFixture(env)
        val request = FakeRequest()

        val result = fixture.controller.me(request)
        status(result) mustBe UNAUTHORIZED
      }
    }
  }

  "GET /users/id" should {
    "return not found if user not found" in {
      forAll { (id: Long) =>
        val env = fakeEnvironment(UserFixture.user)
        val fixture = getFixture(env)
        when(fixture.userService.getByIdWithAuth(id)(UserFixture.user))
          .thenReturn(toErrorResult[User](NotFoundError.User(id)))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id).apply(request)
        status(response) mustBe NOT_FOUND
      }
    }

    "return json with user" in {
      forAll { (id: Long, user: User) =>
        val env = fakeEnvironment(UserFixture.user)
        val fixture = getFixture(env)
        when(fixture.userService.getByIdWithAuth(id)(UserFixture.user)).thenReturn(toSuccessResult(user))
        val request = authenticated(FakeRequest(), env)

        val response = fixture.controller.getById(id)(request)
        status(response) mustBe OK
        val userJson = contentAsJson(response)
        userJson mustBe Json.toJson(ApiShortUser(UserShort.fromUser(user)))
      }
    }
  }

  "GET /users" should {
    "return users list from service" in {
      forAll {
        (
          name: Option[String],
          total: Int,
          users: Seq[User]
        ) =>
          val env = fakeEnvironment(UserFixture.user)
          val fixture = getFixture(env)
          when(fixture.userService.list(None, Some(User.Status.Approved), Tristate.Unspecified, name)(ListMeta.default))
            .thenReturn(toSuccessResult(ListWithTotal(total, users)))
          val request = authenticated(FakeRequest(), env)

          val response = fixture.controller.getList(name)(request)

          status(response) mustBe OK
          val usersJson = contentAsJson(response)
          val expectedJson = Json.toJson(
            Response.List(Response.Meta(total, ListMeta.default), users.map(x => ApiShortUser(UserShort.fromUser(x))))
          )
          usersJson mustBe expectedJson
      }
    }
  }

  private val userToPartial = (_: User)
    .into[ApiPartialUser]
    .withFieldComputed(_.gender, _.gender.map(x => ApiUser.ApiGender(x)))
    .transform

  "PUT /users/current" should {
    "update users" in {
      val env = fakeEnvironment(UserFixture.user)
      val user = UserFixture.user
      val fixture = getFixture(env)
      when(fixture.userService.getById(user.id)).thenReturn(toSuccessResult(user))
      val userCaptor = argumentCaptor[User]
      when(fixture.userService.update(userCaptor.capture(), eqTo(false))(eqTo(user))).thenReturn(toSuccessResult(user))
      val request = authenticated(
        FakeRequest("PUT", "/users/current")
          .withBody[ApiPartialUser](userToPartial(user))
          .withHeaders(CONTENT_TYPE -> "application/json"),
        env
      )

      val response = fixture.controller.update().apply(request)
      val responseJson = contentAsJson(response)
      val expectedJson = Json.toJson(ApiUser(user))

      status(response) mustBe OK
      responseJson mustBe expectedJson
      userCaptor.getValue mustBe user
    }
  }
}
