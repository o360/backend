package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.api.Response
import controllers.api.project.{ApiPartialProject, ApiProject}
import controllers.authorization.{AllowedRole, AllowedStatus}
import services.ProjectService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

/**
  * Project controller.
  */
@Singleton
class ProjectController @Inject()(
  protected val silhouette: Silhouette[DefaultEnv],
  protected val projectService: ProjectService
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields('id, 'name, 'description)

  /**
    * Returns list of projects with relations.
    */
  def getList(eventId: Option[Long], groupId: Option[Long], onlyAvailable: Boolean) =
    (silhouette.SecuredAction(AllowedStatus.approved) andThen ListAction).async { implicit request =>
      toResult(Ok) {
        projectService
          .getList(eventId, groupId, onlyAvailable)
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

  /**
    * Creates project.
    */
  def create = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialProject]) { implicit request =>
    toResult(Created) {
      val project = request.body.toModel()
      projectService
        .create(project)
        .map(ApiProject(_))
    }
  }

  /**
    * Updates project.
    */
  def update(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialProject]) {
    implicit request =>
      toResult(Ok) {
        val project = request.body.toModel(id)
        projectService
          .update(project)
          .map(ApiProject(_))
      }
  }

  /**
    * Removes project.
    */
  def delete(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    projectService
      .delete(id)
      .fold(
        error => toResult(error),
        _ => NoContent
      )
  }
}
