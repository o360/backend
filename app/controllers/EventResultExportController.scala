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

package controllers

import javax.inject.Inject

import controllers.api.Response
import controllers.api.competence.{ApiCompetence, ApiCompetenceGroup}
import controllers.api.export.{ApiAnswerExport, ApiEventExport, ApiExportCode, ApiShortEvent}
import controllers.api.form.ApiForm
import controllers.api.user.ApiShortUser
import models.user.UserShort
import play.api.mvc.ControllerComponents
import services.event.EventResultExportService
import utils.Config
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.concurrent.ExecutionContext

/**
  * Controller exporting event results as JSON.
  */
class EventResultExportController @Inject() (
  config: Config,
  exportService: EventResultExportService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields("id", "start", "end", "description")

  def listEvents = (Action andThen UnsecuredListAction).async(parse.json[ApiExportCode]) { implicit request =>
    if (config.exportSecret != request.body.code) {
      Forbidden.toFuture
    } else {
      exportService.eventsList
        .map { events =>
          Response.List(events) { event =>
            ApiShortEvent(event)
          }
        }
        .map(toResult(_))
    }
  }

  def export(eventId: Long) = Action.async(parse.json[ApiExportCode]) { implicit request =>
    if (config.exportSecret != request.body.code) {
      Forbidden.toFuture
    } else {
      exportService
        .exportAnswers(eventId)
        .map {
          case (forms, users, answers, competencies, competenceGroups) =>
            val apiForms = forms.map(ApiForm(_, includeCompetencies = true))
            val apiUsers = users.map(x => ApiShortUser(UserShort.fromUser(x)))
            val apiAnswers = answers.map(ApiAnswerExport(_))
            val apiCompetencies = competencies.map(ApiCompetence.fromModel)
            val apiCompetenceGroups = competenceGroups.map(ApiCompetenceGroup.fromModel)
            ApiEventExport(apiForms, apiUsers, apiAnswers, apiCompetencies, apiCompetenceGroups)
        }
        .map(toResult(_))
    }
  }
}
