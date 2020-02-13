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
import controllers.api.assessment.{ApiAssessment, ApiPartialAssessment}
import controllers.authorization.AllowedStatus
import play.api.mvc.ControllerComponents
import services.AssessmentService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta
import utils.listmeta.actions.ListActions

import scala.concurrent.ExecutionContext

/**
  * Assessment controller.
  */
@Singleton
class AssessmentController @Inject() (
  silhouette: Silhouette[DefaultEnv],
  assessmentService: AssessmentService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  /**
    * Returns list of available assessment objects.
    */
  def getList(projectId: Long) = silhouette.SecuredAction(AllowedStatus.approved).async { implicit request =>
    toResult(Ok) {
      assessmentService
        .getList(projectId)
        .map { assessmentObjects =>
          Response.List(assessmentObjects)(ApiAssessment(_))(ListMeta.default)
        }
    }
  }

  /**
    * Submits form answers for given event and project ids.
    */
  def bulkSubmit(projectId: Long) =
    silhouette.SecuredAction(AllowedStatus.approved).async(parse.json[Seq[ApiPartialAssessment]]) { implicit request =>
      val model = request.body.map(_.toModel)
      assessmentService
        .bulkSubmit(projectId, model)
        .fold(
          error => toResult(error),
          _ => NoContent
        )
    }
}
