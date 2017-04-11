package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.exceptions.SilhouetteException
import com.mohiva.play.silhouette.impl.providers.{CommonSocialProfileBuilder, SocialProvider, SocialProviderRegistry}
import controllers.api.user.ApiUser
import play.api.libs.json.Json
import play.api.mvc.Action
import services.UserService
import silhouette.DefaultEnv
import utils.errors.AuthenticationError

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

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
    async {
      socialProviderRegistry.get[SocialProvider](provider) match {
        case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
          await(p.authenticate) match {
            case Left(_) => toResult(AuthenticationError.General)
            case Right(authInfo) =>
              val profile = await(p.retrieveProfile(authInfo))
              await(userService.createIfNotExist(profile))
              val authenticator = await(silhouette.env.authenticatorService.create(profile.loginInfo))
              val token = await(silhouette.env.authenticatorService.init(authenticator))
              Ok(Json.obj("token" -> token))
          }
        case _ => toResult(AuthenticationError.ProviderNotSupported(provider))
      }
    }.recover {
      case _: SilhouetteException => toResult(AuthenticationError.General)
    }
  }
}
