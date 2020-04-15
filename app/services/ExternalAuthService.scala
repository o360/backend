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

import com.mohiva.play.silhouette.api.LoginInfo
import javax.inject.{Inject, Singleton}
import models.user.User
import play.api.libs.json.{Json, Reads}
import play.api.libs.ws.WSClient
import scalaz.EitherT
import silhouette.CustomSocialProfile
import utils.Config
import utils.errors.{ApplicationError, AuthenticationError}
import utils.implicits.FutureLifting._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class ExternalAuthService @Inject() (
  config: Config,
  ws: WSClient,
  implicit val ec: ExecutionContext
) extends ServiceResults[ExternalAuthService.ExternalAuthenticationResponse] {
  private val externalUrl = config.externalAuthServerUrl

  def auth(username: String, password: String): SingleResult =
    for {
      url <- externalUrl match {
        case Some(value) => EitherT.pure[Future, ApplicationError, String](value)
        case None        => EitherT.pureLeft[Future, ApplicationError, String](AuthenticationError.General)
      }
      response <- ws
        .url(url)
        .post(Json.obj("username" -> username, "password" -> password))
        .lift
      _ <- ensure(response.status == 200)(AuthenticationError.General)
      authResponse <- response.status match {
        case 200 => response.json.as[ExternalAuthService.ExternalAuthenticationResponse].lift
        case _ =>
          EitherT.pureLeft[Future, ApplicationError, ExternalAuthService.ExternalAuthenticationResponse](
            AuthenticationError.General
          )
      }
    } yield authResponse

  def isEnabled(): Boolean = externalUrl.isDefined

}

object ExternalAuthService {
  case class ExternalAuthenticationResponse(
    userId: String,
    email: Option[String],
    firstName: Option[String],
    lastName: Option[String],
    gender: Option[String]
  ) {
    def toSocialProfile: Try[CustomSocialProfile] = Try {
      CustomSocialProfile(
        loginInfo = LoginInfo("custom", userId),
        firstName = firstName,
        lastName = lastName,
        gender = gender.flatMap { g =>
          g.toLowerCase match {
            case "m" | "man" | "male"     => Some(User.Gender.Male)
            case "f" | "woman" | "female" => Some(User.Gender.Female)
            case unknown                  => throw new RuntimeException(s"Bad gender value '$unknown'")
          }
        },
        email = email
      )
    }
  }
  object ExternalAuthenticationResponse {
    implicit val reads: Reads[ExternalAuthenticationResponse] = Json.reads[ExternalAuthenticationResponse]
  }
}
