package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.exceptions.SilhouetteException
import com.mohiva.play.silhouette.impl.providers.{SocialProvider, SocialProviderRegistry}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.api.test._
import services.UserService
import silhouette.DefaultEnv
import testutils.generator.UserGenerator

import scala.concurrent.Future

/**
  * Authentication controller test.
  */
class AuthenticationControllerTest extends BaseControllerTest with UserGenerator {

  private val socialProviderRegistryMock = mock[SocialProviderRegistry]
  private val silhouetteMock = mock[Silhouette[DefaultEnv]]
  private val userServiceMock = mock[UserService]

  "POST /auth" should {
    "return unauthorized if provider not supported" in {
      forAll { (unsupportedProvider: String) =>
        when(socialProviderRegistryMock.get[SocialProvider](*[String])(*)).thenReturn(None)

        val controller = new Authentication(silhouetteMock, socialProviderRegistryMock, userServiceMock, cc, ec)

        val result = controller.auth(unsupportedProvider)(FakeRequest())
        status(result) mustBe UNAUTHORIZED
      }
    }

    "return unauthorized if exception thrown by silhouette" in {
      val socialProviderMock = mock[SocialProvider]
      when(socialProviderMock.authenticate()(any())).thenReturn(Future.failed(new SilhouetteException("")))
      when(socialProviderRegistryMock.get[SocialProvider](*[String])(*)).thenReturn(Some(socialProviderMock))

      val controller = new Authentication(silhouetteMock, socialProviderRegistryMock, userServiceMock, cc, ec)

      val result = controller.auth("anyprovider")(FakeRequest())
      status(result) mustBe UNAUTHORIZED
    }
  }
}
