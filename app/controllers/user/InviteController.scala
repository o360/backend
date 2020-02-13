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
import controllers.api.invite.ApiInviteCode
import play.api.mvc.ControllerComponents
import services.InviteService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._

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
) extends BaseController {

  /**
    * Submits invite code.
    */
  def submit = silhouette.SecuredAction.async(parse.json[ApiInviteCode]) { implicit request =>
    inviteService
      .applyInvite(request.body.code)
      .fold(
        toResult(_),
        _ => NoContent
      )
  }
}
