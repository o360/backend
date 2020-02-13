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
import controllers.api.form.{ApiForm, ApiPartialForm}
import controllers.authorization.AllowedRole
import play.api.mvc.ControllerComponents
import services.FormService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.concurrent.ExecutionContext

/**
  * Form template controller.
  */
@Singleton
class FormController @Inject() (
  protected val silhouette: Silhouette[DefaultEnv],
  protected val formService: FormService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields("id", "name")

  /**
    * Returns list of form templates without elements.
    */
  def getList = (silhouette.SecuredAction(AllowedRole.admin) andThen ListAction).async { implicit request =>
    toResult(Ok) {
      formService
        .getList()
        .map { forms =>
          Response.List(forms)(ApiForm(_))
        }
    }
  }

  /**
    * Returns form template with elements.
    */
  def getById(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async {
    toResult(Ok) {
      formService
        .getById(id)
        .map(ApiForm(_, includeCompetencies = true))
    }
  }

  /**
    * Creates form template.
    */
  def create = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialForm]) { implicit request =>
    toResult(Created) {
      val form = request.body.toModel()
      formService
        .create(form)
        .map(ApiForm(_, includeCompetencies = true))
    }
  }

  /**
    * Updates form template.
    */
  def update(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialForm]) {
    implicit request =>
      toResult(Ok) {
        val form = request.body.toModel(id)
        formService
          .update(form)
          .map(ApiForm(_, includeCompetencies = true))
      }
  }

  /**
    * Removes form template.
    */
  def delete(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async {
    formService
      .delete(id)
      .fold(
        error => toResult(error),
        _ => NoContent
      )
  }

  /**
    * Clones form.
    */
  def cloneForm(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async {
    toResult(Created) {
      for {
        original <- formService.getById(id)
        created <- formService.create(original.copy(id = 0))
      } yield ApiForm(created, includeCompetencies = true)
    }
  }
}
