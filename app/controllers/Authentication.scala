package controllers

import javax.inject.Inject
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.exceptions.SilhouetteException
import com.mohiva.play.silhouette.impl.providers.{SocialProvider, SocialProviderRegistry}
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import services.UserService
import silhouette.{CustomSocialProfile, DefaultEnv}
import utils.Logger
import utils.errors.AuthenticationError
import utils.implicits.FutureLifting._

import scala.concurrent.ExecutionContext

/**
  * Authentication controller.
  */
class Authentication @Inject()(
  silhouette: Silhouette[DefaultEnv],
  socialProviderRegistry: SocialProviderRegistry,
  userService: UserService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController with Logger {

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
          }
          _ <- userService.createIfNotExist(customProfile)
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
