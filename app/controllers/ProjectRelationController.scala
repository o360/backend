package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.api.Response
import controllers.api.project.{ApiPartialRelation, ApiRelation}
import controllers.authorization.AllowedRole
import services.ProjectRelationService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

/**
  * Project relation controller.
  */
@Singleton
class ProjectRelationController @Inject()(
  protected val silhouette: Silhouette[DefaultEnv],
  protected val projectRelationService: ProjectRelationService
) extends BaseController with ListActions {

  implicit val sortingFields = Sorting.AvailableFields('id, 'projectId)

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
  def getById(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
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
  def update(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialRelation]) { implicit request =>
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
  def delete(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    projectRelationService.delete(id).fold(
      error => toResult(error),
      _ => NoContent
    )
  }
}
