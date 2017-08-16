package controllers

import javax.inject.Inject

import controllers.api.Response
import controllers.api.export.{ApiAnswerExport, ApiEventExport, ApiExportCode, ApiShortEvent}
import controllers.api.form.ApiForm
import controllers.api.user.ApiShortUser
import models.user.UserShort
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Action
import services.EventResultExportService
import utils.Config
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

/**
  * Controller exporting event results as JSON.
  */
class EventResultExportController @Inject()(
  config: Config,
  exportService: EventResultExportService
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields('id, 'start, 'end, 'description)

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
          case (forms, users, answers) =>
            val apiForms = forms.map(ApiForm(_))
            val apiUsers = users.map(x => ApiShortUser(UserShort.fromUser(x)))
            val apiAnswers = answers.map(ApiAnswerExport(_))
            ApiEventExport(apiForms, apiUsers, apiAnswers)
        }
        .map(toResult(_))
    }
  }
}
