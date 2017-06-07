package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.api.Response
import controllers.api.event.{ApiEvent, ApiPartialEvent}
import controllers.authorization.{AllowedRole, AllowedStatus}
import services.EventService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

/**
  * Event controller.
  */
@Singleton
class EventController @Inject()(
  silhouette: Silhouette[DefaultEnv],
  eventService: EventService
) extends BaseController with ListActions {

  implicit val sortingFields = Sorting.AvailableFields('id, 'start, 'end, 'description, 'canRevote)

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
    projectId: Option[Long],
    onlyAvailable: Boolean
  ) = (silhouette.SecuredAction(AllowedStatus.approved) andThen ListAction).async { implicit request =>
    toResult(Ok) {
      eventService
        .list(status.map(_.value), projectId, onlyAvailable)
        .map { events =>
          Response.List(events) {
            event => ApiEvent(event)
          }
        }
    }
  }

  /**
    * Creates event and returns its model.
    */
  def create = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialEvent]) { implicit request =>
    toResult(Created) {
      val event = request.body.toModel()
      eventService
        .create(event)
        .map(ApiEvent(_))
    }
  }

  /**
    * Updates event and returns its model.
    */
  def update(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialEvent]) { implicit request =>
    toResult(Ok) {
      val draft = request.body.toModel(id)
      eventService
        .update(draft)
        .map(ApiEvent(_))
    }
  }

  /**
    * Removes event.
    */
  def delete(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    eventService.delete(id).fold(
      error => toResult(error),
      _ => NoContent
    )
  }

  /**
    * Clones event.
    */
  def cloneEvent(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    toResult(Ok){
      eventService
        .cloneEvent(id)
        .map(ApiEvent(_))
    }
  }
}
