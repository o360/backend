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

import javax.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.exceptions.SilhouetteException
import com.mohiva.play.silhouette.impl.providers.{SocialProvider, SocialProviderRegistry}
import controllers.api.auth.{ApiAuthentication, ApiToken}
import play.api.mvc.{ControllerComponents, RequestHeader, Result}
import play.api.libs.json.{JsArray, JsString}
import services.UserService
import silhouette.{CustomSocialProfile, DefaultEnv}
import utils.Logger
import utils.errors.AuthenticationError
import utils.implicits.FutureLifting._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import services.ExternalAuthService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile

/**
  * Authentication controller.
  */
class Authentication @Inject() (
  silhouette: Silhouette[DefaultEnv],
  socialProviderRegistry: SocialProviderRegistry,
  userService: UserService,
  externalAuthService: ExternalAuthService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with Logger {

  /**
    * Authenticate user by username and password.
    *
    * @return JWT token
    */
  def authCreds() = Action.async(parse.json[ApiAuthentication]) { implicit request =>
    toResult(Ok) {
      for {
        authResponse <- externalAuthService.auth(request.body.username, request.body.password)
        socialProfile <- Future.fromTry(authResponse.toSocialProfile).lift
        token <- retrieveToken(socialProfile).lift
      } yield ApiToken(token)
    }
  }

  /**
    * Authenticate user by OAuth code.
    *
    * @param provider OAuth provider name
    * @return JWT token
    */
  def auth(provider: String) = Action.async { implicit request =>
    def retrieveToken(
      p: SocialProvider,
      authResult: Either[Result, SocialProvider#A]
    ) = authResult match {
      case Left(_) => toResult(AuthenticationError.General).toFuture
      case Right(authInfo) =>
        for {
          profile <- p.retrieveProfile(authInfo.asInstanceOf[p.A])
          customProfile = profile match {
            case p: CustomSocialProfile => p
            case p: CommonSocialProfile => CustomSocialProfile.fromCommonSocialProfile(p)
          }
          token <- this.retrieveToken(customProfile)
        } yield toResult(ApiToken(token))
    }

    socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(p: SocialProvider) =>
        val resultF = for {
          authResult <- p.authenticate()
          result <- retrieveToken(p, authResult)
        } yield result

        resultF.recover {
          case _: SilhouetteException => toResult(AuthenticationError.General)
        }

      case _ => toResult(AuthenticationError.ProviderNotSupported(provider)).toFuture
    }
  }

  /**
    * @return List of available providers.
    */
  def listProviders() = Action {
    val socialProviders = socialProviderRegistry.providers.map(_.id.toUpperCase)

    val allProviders = if (externalAuthService.isEnabled()) {
      "CREDENTIALS" +: socialProviders
    } else {
      socialProviders
    }

    Ok(JsArray(allProviders.map(JsString)))
  }

  private def retrieveToken(profile: CustomSocialProfile)(implicit request: RequestHeader): Future[String] =
    for {
      _ <- userService.createIfNotExist(profile)
      authenticator <- silhouette.env.authenticatorService.create(profile.loginInfo)
      token <- silhouette.env.authenticatorService.init(authenticator)
    } yield token

}
