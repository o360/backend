package controllers

import com.mohiva.play.silhouette.api.exceptions.SilhouetteException
import com.mohiva.play.silhouette.api.{LoginInfo, Silhouette}
import com.mohiva.play.silhouette.impl.providers.{SocialProvider, SocialProviderRegistry}
import com.mohiva.play.silhouette.test._
import controllers.api.user.UserResponse
import models.user.{User => UserModel}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import services.{User => UserService}
import silhouette.DefaultEnv
import testutils.generator.UserGenerator

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Authentication controller test.
  */
class AuthenticationControllerTest extends BaseControllerTest with UserGenerator {

  private val socialProviderRegistryMock = mock[SocialProviderRegistry]
  private val silhouetteMock = mock[Silhouette[DefaultEnv]]
  private val userServiceMock = mock[UserService]

  "GET /auth" should {
    "return user's json if user is present" in {
      forAll { (user: UserModel, provId: String, provKey: String) =>
        implicit val env = FakeEnvironment[DefaultEnv](Seq(LoginInfo(provId, provKey) -> user))
        val controller = new Authentication(getSilhouette(env), socialProviderRegistryMock, userServiceMock)
        val request = FakeRequest().withAuthenticator(LoginInfo(provId, provKey))

        val result = controller.me(request)
        status(result) mustBe OK
        val userJson = contentAsJson(result)
        userJson mustBe Json.toJson(UserResponse(user))
      }
    }
    "return unauthorized if user is not present" in {
      forAll { (provId: String, provKey: String) =>
        implicit val env = FakeEnvironment[DefaultEnv](Nil)
        val controller = new Authentication(getSilhouette(env), socialProviderRegistryMock, userServiceMock)
        val request = FakeRequest().withAuthenticator(LoginInfo(provId, provKey))

        val result = controller.me(request)
        status(result) mustBe UNAUTHORIZED
      }
    }

    "return unauthorized if authenticator is not present" in {
      forAll { (user: UserModel, provId: String, provKey: String) =>
        implicit val env = FakeEnvironment[DefaultEnv](Seq(LoginInfo(provId, provKey) -> user))
        val controller = new Authentication(getSilhouette(env), socialProviderRegistryMock, userServiceMock)
        val request = FakeRequest()

        val result = controller.me(request)
        status(result) mustBe UNAUTHORIZED
      }
    }
  }

  "POST /auth" should {
    "return unauthorized if provider not supported" in {
      forAll { (unsupportedProvider: String) =>
        when(socialProviderRegistryMock.get[SocialProvider](any[String])(any())).thenReturn(None)

        val controller = new Authentication(silhouetteMock, socialProviderRegistryMock, userServiceMock)

        val result = controller.auth(unsupportedProvider)(FakeRequest())
        status(result) mustBe UNAUTHORIZED
      }
    }

    "return unauthorized if exception thrown by silhouette" in {
      when(socialProviderRegistryMock.get[SocialProvider](any[String])(any())).thenThrow(classOf[SilhouetteException])

      val controller = new Authentication(silhouetteMock, socialProviderRegistryMock, userServiceMock)

      val result = controller.auth("anyprovider")(FakeRequest())
      status(result) mustBe UNAUTHORIZED
    }
  }
}
