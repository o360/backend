/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.admin

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.event.{ApiEvent, ApiPartialEvent}
import controllers.authorization.AllowedRole
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
        .list(status.map(_.value), projectId)
        .map { events =>
          Response.List(events) { event =>
            ApiEvent(event)
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
  def update(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialEvent]) {
    implicit request =>
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
  def delete(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async {
    eventService
      .delete(id)
      .fold(
        error => toResult(error),
        _ => NoContent
      )
  }

  /**
    * Clones event.
    */
  def cloneEvent(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    toResult(Ok) {
      eventService
        .cloneEvent(id)
        .map(ApiEvent(_))
    }
  }
}
