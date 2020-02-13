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
import services.event.EventProjectService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._

import scala.concurrent.ExecutionContext

/**
  * Event project controller.
  */
@Singleton
class EventProjectController @Inject() (
  protected val silhouette: Silhouette[DefaultEnv],
  protected val eventProjectService: EventProjectService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController {

  /**
    * Adds event to project.
    *
    * @param projectId project ID
    * @param eventId   event ID
    */
  def create(projectId: Long, eventId: Long) = silhouette.SecuredAction(AllowedRole.admin).async {
    eventProjectService
      .add(projectId, eventId)
      .fold(
        error => toResult(error),
        _ => NoContent
      )
  }

  /**
    * Removes event from project.
    *
    * @param projectId project ID
    * @param eventId   event ID
    */
  def delete(projectId: Long, eventId: Long) = silhouette.SecuredAction(AllowedRole.admin).async {
    eventProjectService
      .remove(projectId, eventId)
      .fold(
        error => toResult(error),
        _ => NoContent
      )
  }
}
