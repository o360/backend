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

import com.mohiva.play.silhouette.api.{AuthInfo, LoginInfo}
import com.mohiva.play.silhouette.impl.providers.{SocialProfile, SocialProfileParser}
import models.user.User
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile

/**
  * Custom silhouette social profile.
  */
case class CustomSocialProfile(
  loginInfo: LoginInfo,
  firstName: Option[String],
  lastName: Option[String],
  email: Option[String],
  gender: Option[User.Gender]
) extends SocialProfile

object CustomSocialProfile {
  def fromCommonSocialProfile(profile: CommonSocialProfile): CustomSocialProfile =
    CustomSocialProfile(
      loginInfo = profile.loginInfo,
      firstName = profile.firstName,
      lastName = profile.lastName,
      email = profile.email,
      gender = None
    )
}

/**
  * Profile parser for custom social profile.
  */
trait CustomSocialProfileParser[C, A <: AuthInfo] extends SocialProfileParser[C, CustomSocialProfile, A]
