package controllers.admin

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.authorization.AllowedRole
import play.api.mvc.ControllerComponents
import services.EventProjectService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._

import scala.concurrent.ExecutionContext

/**
  * Event project controller.
  */
@Singleton
class EventProjectController @Inject()(
  protected val silhouette: Silhouette[DefaultEnv],
  protected val eventProjectService: EventProjectService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
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
