package silhouette

import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth2.{BaseGoogleProvider, GoogleProfileParser}
import models.user.User
import play.api.libs.json.JsValue

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

/**
  * Custom google profile parser.
  */
class CustomGoogleProfileParser extends CustomSocialProfileParser[JsValue, OAuth2Info] {

  val parser = new GoogleProfileParser

  def parse(content: JsValue, authInfo: OAuth2Info): Future[CustomSocialProfile] = {
    parser.parse(content, authInfo).map { commonProfile =>
      val gender = (content \ "gender").asOpt[String] match {
        case Some("male") => Some(User.Gender.Male)
        case Some("female") => Some(User.Gender.Female)
        case _ => None
      }

      CustomSocialProfile(
        loginInfo = commonProfile.loginInfo,
        fullName = commonProfile.fullName,
        email = commonProfile.email,
        gender = gender
      )
    }
  }
}
/**
  * Custom google provider.
  */
class CustomGoogleProvider(
  protected val httpLayer: HTTPLayer,
  protected val stateProvider: OAuth2StateProvider,
  val settings: OAuth2Settings
) extends BaseGoogleProvider {

  type Self = CustomGoogleProvider
  type Profile = CustomSocialProfile

  def withSettings(f: (OAuth2Settings) => OAuth2Settings): CustomGoogleProvider = {
    new CustomGoogleProvider(httpLayer, stateProvider, f(settings))
  }

  protected def profileParser: CustomGoogleProfileParser = new CustomGoogleProfileParser
}
