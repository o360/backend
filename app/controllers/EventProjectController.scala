package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.authorization.AllowedRole
import services.EventProjectService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._

/**
  * Event project controller.
  */
@Singleton
class EventProjectController @Inject()(
  protected val silhouette: Silhouette[DefaultEnv],
  protected val eventProjectService: EventProjectService
) extends BaseController {

  /**
    * Adds event to project.
    *
    * @param projectId project ID
    * @param eventId   event ID
    */
  def create(projectId: Long, eventId: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    eventProjectService
      .add(projectId, eventId)
      .fold(
        error => toResult(error),
        _ => NoContent
      )
  }

  /**
    * Removes event from project.
    *
    * @param projectId project ID
    * @param eventId   event ID
    */
  def delete(projectId: Long, eventId: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    eventProjectService
      .remove(projectId, eventId)
      .fold(
        error => toResult(error),
        _ => NoContent
      )
  }
}
