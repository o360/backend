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
import controllers.authorization.AllowedRole
import play.api.mvc.ControllerComponents
import services.event.EventJobService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions

import scala.concurrent.ExecutionContext

/**
  * Event job controller.
  */
@Singleton
class EventJobController @Inject() (
  silhouette: Silhouette[DefaultEnv],
  eventJobService: EventJobService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  /**
    * Restarts failed job.
    */
  def restart(jobId: Long) = silhouette.SecuredAction(AllowedRole.admin).async {
    eventJobService
      .runFailedJob(jobId)
      .fold(
        toResult(_),
        _ => NoContent
      )
  }
}
