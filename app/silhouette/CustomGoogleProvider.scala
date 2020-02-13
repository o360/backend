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

package silhouette

import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth2.{BaseGoogleProvider, GoogleProfileParser}
import models.user.User
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}

/**
  * Custom google profile parser.
  */
class CustomGoogleProfileParser(implicit ec: ExecutionContext) extends CustomSocialProfileParser[JsValue, OAuth2Info] {

  val parser = new GoogleProfileParser

  def parse(content: JsValue, authInfo: OAuth2Info): Future[CustomSocialProfile] = {
    parser.parse(content, authInfo).map { commonProfile =>
      val gender = (content \ "gender").asOpt[String].flatMap {
        case "male"   => Some(User.Gender.Male)
        case "female" => Some(User.Gender.Female)
        case _        => None
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
  protected val stateHandler: SocialStateHandler,
  val settings: OAuth2Settings,
  implicit val ec: ExecutionContext
) extends BaseGoogleProvider {

  type Self = CustomGoogleProvider
  type Profile = CustomSocialProfile

  def withSettings(f: (OAuth2Settings) => OAuth2Settings): CustomGoogleProvider = {
    new CustomGoogleProvider(httpLayer, stateHandler, f(settings), ec)
  }

  protected def profileParser: CustomGoogleProfileParser = new CustomGoogleProfileParser
}
