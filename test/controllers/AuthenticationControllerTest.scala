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
