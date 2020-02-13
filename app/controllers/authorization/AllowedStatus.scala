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
  * Status authorization. Access is allowed only for given statuses.
  */
case class AllowedStatus(statuses: User.Status*) extends Authorization[User, JWTAuthenticator] {
  override def isAuthorized[B](
    identity: User,
    authenticator: JWTAuthenticator
  )(implicit request: Request[B]): Future[Boolean] = {
    Future.successful(statuses.contains(identity.status))
  }
}

case object AllowedStatus {

  /**
    * Allows access only to approved users.
    */
  val approved = AllowedStatus(User.Status.Approved)
}
