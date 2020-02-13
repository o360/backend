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

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.competence.{ApiCompetence, ApiPartialCompetence}
import controllers.authorization.AllowedRole
import play.api.mvc.ControllerComponents
import services.CompetenceService
import silhouette.DefaultEnv
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting
import utils.implicits.FutureLifting._

import scala.concurrent.ExecutionContext

/**
  * Competence controller.
  */
class CompetenceController @Inject() (
  silhouette: Silhouette[DefaultEnv],
  competenceService: CompetenceService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields("id", "name", "groupId", "description")

  private val secured = silhouette.SecuredAction(AllowedRole.admin)

  def create = secured.async(parse.json[ApiPartialCompetence]) { request =>
    toResult(Created) {
      competenceService
        .create(request.body.toModel())
        .map(ApiCompetence.fromModel)
    }
  }

  def getById(id: Long) = secured.async { _ =>
    toResult(Ok) {
      competenceService
        .getById(id)
        .map(ApiCompetence.fromModel)
    }
  }

  def getList(groupId: Option[Long]) = secured.andThen(ListAction).async { implicit request =>
    toResult(Ok) {
      competenceService
        .getList(groupId)
        .map(Response.List(_)(ApiCompetence.fromModel))
    }
  }

  def update(id: Long) = secured.async(parse.json[ApiPartialCompetence]) { request =>
    toResult(Ok) {
      competenceService
        .update(request.body.toModel(id))
        .map(ApiCompetence.fromModel)
    }
  }

  def delete(id: Long) = secured.async { _ =>
    competenceService
      .delete(id)
      .fold(
        toResult(_),
        _ => NoContent
      )
  }

}
