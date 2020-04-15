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
import models.user.User
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}
import com.mohiva.play.silhouette.impl.providers.oauth2.BaseVKProvider
import com.mohiva.play.silhouette.impl.providers.oauth2.VKProfileParser

/**
  * Custom vk profile parser.
  */
class CustomVKProfileParser(implicit ec: ExecutionContext) extends CustomSocialProfileParser[JsValue, OAuth2Info] {

  val parser = new VKProfileParser

  def parse(content: JsValue, authInfo: OAuth2Info): Future[CustomSocialProfile] = {
    parser.parse(content, authInfo).map { commonProfile =>
      val response = (content \ "response").apply(0)
      val gender = (response \ "sex").asOpt[Int].flatMap {
        case 1 => Some(User.Gender.Female)
        case 2 => Some(User.Gender.Male)
        case _ => None
      }

      CustomSocialProfile(
        loginInfo = commonProfile.loginInfo,
        firstName = commonProfile.firstName,
        lastName = commonProfile.lastName,
        email = commonProfile.email,
        gender = gender
      )
    }
  }
}

/**
  * Custom vk provider.
  */
class CustomVKProvider(
  protected val httpLayer: HTTPLayer,
  protected val stateHandler: SocialStateHandler,
  val settings: OAuth2Settings,
  implicit val ec: ExecutionContext
) extends BaseVKProvider {

  type Self = CustomVKProvider
  type Profile = CustomSocialProfile

  def withSettings(f: OAuth2Settings => OAuth2Settings): CustomVKProvider = {
    new CustomVKProvider(httpLayer, stateHandler, f(settings), ec)
  }

  protected def profileParser: CustomVKProfileParser = new CustomVKProfileParser
}
