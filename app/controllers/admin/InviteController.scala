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
import controllers.api.invite.{ApiInvite, ApiPartialInvite}
import controllers.authorization.AllowedRole
import play.api.mvc.ControllerComponents
import services.InviteService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.concurrent.ExecutionContext

/**
  * Invite controller.
  */
@Singleton
class InviteController @Inject() (
  silhouette: Silhouette[DefaultEnv],
  inviteService: InviteService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields("code", "email", "activationTime", "creationTime")

  /**
    * Returns list of invites.
    */
  def getList(activated: Option[Boolean]) = silhouette.SecuredAction(AllowedRole.admin).andThen(ListAction).async {
    implicit request =>
      toResult(Ok) {
        inviteService
          .getList(activated)
          .map(invites =>
            Response.List(invites) { invite =>
              ApiInvite(invite)
            }
          )
      }
  }

  /**
    * Bulk creates invites.
    */
  def bulkCreate = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[Seq[ApiPartialInvite]]) {
    implicit request =>
      inviteService
        .createInvites(request.body.map(_.toModel))
        .fold(
          toResult(_),
          _ => NoContent
        )
  }
}
