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

package controllers.admin

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.user.ApiUser
import controllers.authorization.AllowedRole
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

  implicit val sortingFields = Sorting.AvailableFields("id", "name", "email", "role", "status", "gender")

  /**
    * Returns user by ID.
    */
  def getById(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async {
    toResult(Ok) {
      userService
        .getById(id)
        .map(ApiUser(_))
    }
  }

  /**
    * Returns list of users filtered by given filters.
    */
  def getList(
    role: Option[ApiUser.ApiRole],
    status: Option[ApiUser.ApiStatus],
    groupId: Tristate[Long],
    name: Option[String]
  ) = (silhouette.SecuredAction(AllowedRole.admin) andThen ListAction).async { implicit request =>
    toResult(Ok) {
      userService
        .list(role.map(_.value), status.map(_.value), groupId, name)
        .map { users =>
          Response.List(users) { user =>
            ApiUser(user)
          }
        }
    }
  }

  /**
    * Updates user.
    */
  def update(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiUser]) { implicit request =>
    toResult(Ok) {
      val draft = request.body.copy(id = id)
      userService
        .update(draft.toModel)
        .map(ApiUser(_))
    }
  }

  /**
    * Deletes user.
    */
  def delete(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async {
    userService
      .delete(id)
      .fold(
        error => toResult(error),
        _ => NoContent
      )
  }
}
