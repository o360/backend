package controllers.user

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.event.ApiEvent
import controllers.authorization.AllowedStatus
import play.api.mvc.ControllerComponents
import services.event.EventService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.concurrent.ExecutionContext

/**
  * Event controller.
  */
@Singleton
class EventController @Inject() (
  silhouette: Silhouette[DefaultEnv],
  eventService: EventService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields("id", "start", "end", "description")

  /**
    * Returns event by ID.
    */
  def getById(id: Long) = silhouette.SecuredAction(AllowedStatus.approved).async { implicit request =>
    toResult(Ok) {
      eventService
        .getByIdWithAuth(id)
        .map(ApiEvent(_))
    }
  }

  /**
    * Returns filtered events list.
    */
  def getList(status: Option[ApiEvent.EventStatus]) =
    (silhouette.SecuredAction(AllowedStatus.approved) andThen ListAction).async { implicit request =>
      toResult(Ok) {
        eventService
          .listWithAuth(status.map(_.value))
          .map { events =>
            Response.List(events) { event =>
              ApiEvent(event)
            }
          }
      }
    }
}
