package controllers.user

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.event.ApiEvent
import controllers.authorization.AllowedRole
import play.api.mvc.ControllerComponents
import services.EventService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.concurrent.ExecutionContext

/**
  * Event controller.
  */
@Singleton
class EventController @Inject()(
  silhouette: Silhouette[DefaultEnv],
  eventService: EventService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields('id, 'start, 'end, 'description)

  /**
    * Returns event by ID.
    */
  def getById(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    toResult(Ok) {
      eventService
        .getById(id)
        .map(ApiEvent(_))
    }
  }

  /**
    * Returns filtered events list.
    */
  def getList(
    status: Option[ApiEvent.EventStatus],
    projectId: Option[Long]
  ) = (silhouette.SecuredAction(AllowedRole.admin) andThen ListAction).async { implicit request =>
    toResult(Ok) {
      eventService
        .list(status.map(_.value), projectId, onlyAvailable = true)
        .map { events =>
          Response.List(events) { event =>
            ApiEvent(event)
          }
        }
    }
  }
}
