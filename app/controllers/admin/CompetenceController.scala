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
