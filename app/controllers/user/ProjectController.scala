package controllers.user

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.project.{ApiPartialProject, ApiProject}
import controllers.authorization.{AllowedRole, AllowedStatus}
import play.api.mvc.ControllerComponents
import services.ProjectService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.concurrent.ExecutionContext

/**
  * Project controller.
  */
@Singleton
class ProjectController @Inject()(
  protected val silhouette: Silhouette[DefaultEnv],
  protected val projectService: ProjectService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields('id, 'name, 'description)

  /**
    * Returns list of projects with relations.
    */
  def getList(eventId: Option[Long], groupId: Option[Long]) =
    (silhouette.SecuredAction(AllowedStatus.approved) andThen ListAction).async { implicit request =>
      toResult(Ok) {
        projectService
          .getList(eventId, groupId, onlyAvailable = true)
          .map { projects =>
            Response.List(projects)(ApiProject(_))
          }
      }
    }

  /**
    * Returns project with relations.
    */
  def getById(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    toResult(Ok) {
      projectService
        .getById(id)
        .map(ApiProject(_))
    }
  }
}
