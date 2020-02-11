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
