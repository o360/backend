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

package controllers.authorization

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.user.User
import play.api.mvc.Request

import scala.concurrent.Future

/**
  * Roles authorization. Access is allowed only for given roles.
  */
case class AllowedRole(roles: User.Role*) extends Authorization[User, JWTAuthenticator] {
  override def isAuthorized[B](
    identity: User,
    authenticator: JWTAuthenticator
  )(implicit request: Request[B]): Future[Boolean] = {
    Future.successful(roles.contains(identity.role))
  }
}

case object AllowedRole {

  /**
    * Allows access only to admins.
    */
  def admin = AllowedRole(User.Role.Admin)
}
