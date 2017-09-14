package controllers.user

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.test._
import controllers.BaseControllerTest
import controllers.api.user.ApiUser
import models.user.{User => UserModel}
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import services.UserService
import silhouette.DefaultEnv
import testutils.generator.UserGenerator

/**
  * Authentication controller test.
  */
class UserControllerTest extends BaseControllerTest with UserGenerator {

  private val userServiceMock = mock[UserService]

  "GET /users/current" should {
    "return user's json if user is present" in {
      forAll { (user: UserModel, provId: String, provKey: String) =>
        implicit val env = FakeEnvironment[DefaultEnv](Seq(LoginInfo(provId, provKey) -> user))
        val controller = new UserController(getSilhouette(env), userServiceMock, cc, ec)
        val request = FakeRequest().withAuthenticator(LoginInfo(provId, provKey))

        val result = controller.me(request)
        status(result) mustBe OK
        val userJson = contentAsJson(result)
        userJson mustBe Json.toJson(ApiUser(user))
      }
    }
    "return unauthorized if user is not present" in {
      forAll { (provId: String, provKey: String) =>
        implicit val env = FakeEnvironment[DefaultEnv](Nil)
        val controller = new UserController(getSilhouette(env), userServiceMock, cc, ec)
        val request = FakeRequest().withAuthenticator(LoginInfo(provId, provKey))

        val result = controller.me(request)
        status(result) mustBe UNAUTHORIZED
      }
    }

    "return unauthorized if authenticator is not present" in {
      forAll { (user: UserModel, provId: String, provKey: String) =>
        implicit val env = FakeEnvironment[DefaultEnv](Seq(LoginInfo(provId, provKey) -> user))
        val controller = new UserController(getSilhouette(env), userServiceMock, cc, ec)
        val request = FakeRequest()

        val result = controller.me(request)
        status(result) mustBe UNAUTHORIZED
      }
    }
  }
}
