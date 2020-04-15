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

package controllers.user

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.user.{ApiPartialUser, ApiShortUser, ApiUser}
import controllers.authorization.AllowedStatus
import models.user.{User, UserShort}
import org.davidbild.tristate.Tristate
import play.api.mvc.ControllerComponents
import services.UserService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.concurrent.ExecutionContext

/**
  * User controller.
  */
@Singleton
class UserController @Inject() (
  protected val silhouette: Silhouette[DefaultEnv],
  protected val userService: UserService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields("id", "firstName", "lastName", "gender")

  /**
    * Returns user by ID.
    */
  def getById(id: Long) = silhouette.SecuredAction(AllowedStatus.approved).async { implicit request =>
    toResult(Ok) {
      userService
        .getByIdWithAuth(id)
        .map(x => ApiShortUser(UserShort.fromUser(x)))
    }
  }

  /**
    * Returns list of users filtered by given filters.
    */
  def getList(name: Option[String]) = (silhouette.SecuredAction(AllowedStatus.approved) andThen ListAction).async {
    implicit request =>
      toResult(Ok) {
        userService
          .list(None, Some(User.Status.Approved), Tristate.Unspecified, name)
          .map { users =>
            Response.List(users) { user =>
              ApiShortUser(UserShort.fromUser(user))
            }
          }
      }
  }

  /**
    * Returns logged in user.
    */
  def me = silhouette.SecuredAction { request =>
    toResult(ApiUser(request.identity))
  }

  /**
    * Updates user.
    */
  def update = silhouette.SecuredAction.async(parse.json[ApiPartialUser]) { implicit request =>
    toResult(Ok) {
      for {
        user <- userService.getById(request.identity.id)
        patched = request.body.applyTo(user)
        updated <- userService.update(patched)
      } yield ApiUser(updated)
    }
  }
}
