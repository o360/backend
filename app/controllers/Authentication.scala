package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.exceptions.SilhouetteException
import com.mohiva.play.silhouette.impl.providers.{CommonSocialProfileBuilder, SocialProvider, SocialProviderRegistry}
import controllers.api.user.ApiUser
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, Result}
import services.UserService
import silhouette.{CustomSocialProfile, DefaultEnv}
import utils.errors.AuthenticationError
import utils.implicits.FutureLifting._

/**
  * Authentication controller.
  */
class Authentication @Inject()(
  silhouette: Silhouette[DefaultEnv],
  socialProviderRegistry: SocialProviderRegistry,
  userService: UserService
) extends BaseController {

  /**
    * Returns logged in user.
    */
  def me = silhouette.SecuredAction { request =>
    toResult(ApiUser(request.identity))
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
          _ <- userService.createIfNotExist(profile.asInstanceOf[CustomSocialProfile])
          authenticator <- silhouette.env.authenticatorService.create(profile.loginInfo)
          token <- silhouette.env.authenticatorService.init(authenticator)
        } yield Ok(Json.obj("token" -> token))
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
}
