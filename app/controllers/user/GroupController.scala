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
import controllers.api.group.ApiGroup
import controllers.authorization.AllowedStatus
import play.api.mvc.ControllerComponents
import services.{GroupService, UserService}
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta
import utils.listmeta.actions.ListActions

import scala.concurrent.ExecutionContext

/**
  * Group controller.
  */
@Singleton
class GroupController @Inject() (
  silhouette: Silhouette[DefaultEnv],
  groupService: GroupService,
  userService: UserService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  /**
    * Returns current user groups.
    */
  def getList =
    silhouette.SecuredAction(AllowedStatus.approved).async { implicit request =>
      getListByUserId(request.identity.id)(request)
    }

  /**
    * Returns list of groups user participates in.
    */
  def getListByUserId(userId: Long) = silhouette.SecuredAction(AllowedStatus.approved).async { implicit request =>
    toResult(Ok) {
      userService.getByIdWithAuth(userId).flatMap { _ =>
        groupService
          .listByUserId(userId)
          .map { groups =>
            Response.List(groups) { group =>
              ApiGroup(group)
            }(ListMeta.default)
          }
      }
    }
  }
}
