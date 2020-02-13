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
import controllers.api.project.{ApiPartialRelation, ApiRelation}
import controllers.authorization.AllowedRole
import play.api.mvc.ControllerComponents
import services.ProjectRelationService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.concurrent.ExecutionContext

/**
  * Project relation controller.
  */
@Singleton
class ProjectRelationController @Inject() (
  protected val silhouette: Silhouette[DefaultEnv],
  protected val projectRelationService: ProjectRelationService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields("id", "projectId")

  /**
    * Returns list of relations with relations.
    */
  def getList(eventId: Option[Long]) =
    (silhouette.SecuredAction(AllowedRole.admin) andThen ListAction).async { implicit request =>
      toResult(Ok) {
        projectRelationService
          .getList(eventId)
          .map { projectRelations =>
            Response.List(projectRelations)(ApiRelation(_))
          }
      }
    }

  /**
    * Returns relation with relations.
    */
  def getById(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async {
    toResult(Ok) {
      projectRelationService
        .getById(id)
        .map(ApiRelation(_))
    }
  }

  /**
    * Creates relation.
    */
  def create = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialRelation]) { implicit request =>
    toResult(Created) {
      val projectRelation = request.body.toModel()
      projectRelationService
        .create(projectRelation)
        .map(ApiRelation(_))
    }
  }

  /**
    * Updates relation.
    */
  def update(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialRelation]) {
    implicit request =>
      toResult(Ok) {
        val projectRelation = request.body.toModel(id)
        projectRelationService
          .update(projectRelation)
          .map(ApiRelation(_))
      }
  }

  /**
    * Removes relation.
    */
  def delete(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async {
    projectRelationService
      .delete(id)
      .fold(
        error => toResult(error),
        _ => NoContent
      )
  }
}
