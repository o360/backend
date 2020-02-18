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

package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.exceptions.SilhouetteException
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.providers.{SocialProvider, SocialProviderRegistry}
import controllers.api.auth.{ApiAuthentication, ApiToken}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import services.UserService
import silhouette.DefaultEnv
import testutils.generator.UserGenerator

import scala.concurrent.Future
import services.ExternalAuthService
import services.ExternalAuthService.ExternalAuthenticationResponse
import utils.errors.AuthenticationError

/**
  * Authentication controller test.
  */
class AuthenticationControllerTest extends BaseControllerTest with UserGenerator {

  private val socialProviderRegistryMock = mock[SocialProviderRegistry]
  private val silhouetteMock = mock[Silhouette[DefaultEnv]](Mockito.RETURNS_DEEP_STUBS)
  private val userServiceMock = mock[UserService]
  private val externalAuthServiceMock = mock[ExternalAuthService]

  "POST /auth" should {
    "return unauthorized if provider not supported" in {
      forAll { unsupportedProvider: String =>
        when(socialProviderRegistryMock.get[SocialProvider](*[String])(*)).thenReturn(None)

        val controller = new Authentication(
          silhouetteMock,
          socialProviderRegistryMock,
          userServiceMock,
          externalAuthServiceMock,
          cc,
          ec
        )

        val result = controller.auth(unsupportedProvider)(FakeRequest())
        status(result) mustBe UNAUTHORIZED
      }
    }

    "return unauthorized if exception thrown by silhouette" in {
      val socialProviderMock = mock[SocialProvider]
      when(socialProviderMock.authenticate()(any())).thenReturn(Future.failed(new SilhouetteException("")))
      when(socialProviderRegistryMock.get[SocialProvider](*[String])(*)).thenReturn(Some(socialProviderMock))

      val controller = new Authentication(
        silhouetteMock,
        socialProviderRegistryMock,
        userServiceMock,
        externalAuthServiceMock,
        cc,
        ec
      )

      val result = controller.auth("anyprovider")(FakeRequest())
      status(result) mustBe UNAUTHORIZED
    }
  }

  "POST /auth-creds" should {
    "return token" in {
      val jwtAuthenticatorMock = mock[JWTAuthenticator]
      when(externalAuthServiceMock.auth(any(), any())).thenReturn(
        toSuccessResult(
          ExternalAuthenticationResponse(
            "uid",
            Some("email"),
            Some("firstName"),
            Some("lastName"),
            Some("m")
          )
        )
      )
      when(userServiceMock.createIfNotExist(any())).thenReturn(toFuture(()))
      when(silhouetteMock.env.authenticatorService.create(any())(any())).thenReturn(toFuture(jwtAuthenticatorMock))
      when(silhouetteMock.env.authenticatorService.init(eqTo(jwtAuthenticatorMock))(any()))
        .thenReturn(toFuture("token"))

      val controller = new Authentication(
        silhouetteMock,
        socialProviderRegistryMock,
        userServiceMock,
        externalAuthServiceMock,
        cc,
        ec
      )

      val request = FakeRequest("POST", "/auth-creds")
        .withBody(ApiAuthentication("user", "pass"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.authCreds()(request)
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(ApiToken("token"))
    }

    "return unauthorized if provider not supported" in {
      when(externalAuthServiceMock.auth(any(), any())).thenReturn(toErrorResult(AuthenticationError.General))
      val controller = new Authentication(
        silhouetteMock,
        socialProviderRegistryMock,
        userServiceMock,
        externalAuthServiceMock,
        cc,
        ec
      )

      val request = FakeRequest("POST", "/auth-creds")
        .withBody(ApiAuthentication("user", "pass"))
        .withHeaders(CONTENT_TYPE -> "application/json")

      val result = controller.authCreds()(request)
      status(result) mustBe UNAUTHORIZED
    }
  }

}
