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
import controllers.api.project.ApiActiveProject
import controllers.authorization.AllowedStatus
import play.api.mvc.ControllerComponents
import services.event.ActiveProjectService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.concurrent.ExecutionContext

/**
  * Controller for active project.
  */
@Singleton
class ActiveProjectController @Inject() (
  protected val silhouette: Silhouette[DefaultEnv],
  protected val projectService: ActiveProjectService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields("id", "name", "description")

  /**
    * Returns list of active projects.
    */
  def getList(eventId: Option[Long]) =
    (silhouette.SecuredAction(AllowedStatus.approved) andThen ListAction).async { implicit request =>
      toResult(Ok) {
        projectService
          .getList(eventId)
          .map { projects =>
            Response.List(projects)(ApiActiveProject(_))
          }
      }
    }

  /**
    * Returns active project.
    */
  def getById(id: Long) = silhouette.SecuredAction(AllowedStatus.approved).async { implicit request =>
    toResult(Ok) {
      projectService
        .getById(id)
        .map(ApiActiveProject(_))
    }
  }

}
