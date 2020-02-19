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

import models.user.User
import org.mockito.Mockito._
import play.api.libs.json.{JsResultException, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.WsTestClient
import play.core.server.Server
import services.ExternalAuthService.ExternalAuthenticationResponse
import testutils.generator.UserGenerator
import utils.Config
import utils.errors.{ApplicationError, AuthenticationError}

/**
  * Test for external authentication service.
  */
class ExternalAuthServiceTest extends BaseServiceTest with UserGenerator {

  def withStubbedWSClient[T](block: => Result)(f: WSClient => T): T =
    Server.withRouterFromComponents() { components =>
      {
        case _ => components.defaultActionBuilder(block)
      }
    } { implicit port =>
      WsTestClient.withClient { ws =>
        f(ws)
      }
    }

  "auth" should {
    "return authentication response" in forAll {
      (
        userId: String,
        firstName: Option[String],
        lastName: Option[String],
        email: Option[String],
        gender: Option[User.Gender]
      ) =>
        val genderString = gender.map {
          case User.Gender.Female => "f"
          case User.Gender.Male   => "m"
        }
        withStubbedWSClient(
          Ok(
            Json.obj(
              "userId" -> userId,
              "firstName" -> firstName,
              "lastName" -> lastName,
              "email" -> email,
              "gender" -> genderString
            )
          )
        ) { ws =>
          val config = mock[Config]
          when(config.externalAuthServerUrl).thenReturn(Some("/"))

          val service = new ExternalAuthService(config, ws, ec)

          val result = wait(service.auth("username", "password").run)
          result must beRight(
            ExternalAuthenticationResponse(
              userId = userId,
              email = email,
              firstName = firstName,
              lastName = lastName,
              gender = genderString
            )
          )
        }
    }

    "return authentication error if url is not configured" in {
      val config = mock[Config]
      val ws = mock[WSClient]

      when(config.externalAuthServerUrl).thenReturn(None)

      val service = new ExternalAuthService(config, ws, ec)
      val result = wait(service.auth("username", "password").run)
      result must beLeft[ApplicationError](AuthenticationError.General)
    }

    "return authentication error if server returns not 200 code" in {
      withStubbedWSClient(NotFound) { ws =>
        val config = mock[Config]
        when(config.externalAuthServerUrl).thenReturn(Some("/"))

        val service = new ExternalAuthService(config, ws, ec)
        val result = wait(service.auth("username", "password").run)
        result must beLeft[ApplicationError](AuthenticationError.General)
      }
    }

    "fail if server returns 200 code with invalid body" in {
      withStubbedWSClient(Ok(Json.obj())) { ws =>
        val config = mock[Config]
        when(config.externalAuthServerUrl).thenReturn(Some("/"))

        val service = new ExternalAuthService(config, ws, ec)
        val resultF = service.auth("username", "password").run

        intercept[JsResultException](wait(resultF))
      }
    }
  }

}
