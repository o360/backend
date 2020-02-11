package controllers.admin

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.BaseController
import controllers.api.Response
import controllers.api.group.{ApiGroup, ApiPartialGroup}
import controllers.authorization.AllowedRole
import org.davidbild.tristate.Tristate
import play.api.mvc.ControllerComponents
import services.GroupService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.concurrent.ExecutionContext

/**
  * Group controller.
  */
@Singleton
class GroupController @Inject() (
  silhouette: Silhouette[DefaultEnv],
  groupService: GroupService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController
  with ListActions {

  implicit val sortingFields = Sorting.AvailableFields("id", "name")

  /**
    * Returns group by ID.
    */
  def getById(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async {
    toResult(Ok) {
      groupService
        .getById(id)
        .map(ApiGroup(_))
    }
  }

  /**
    * Returns filtered groups list.
    */
  def getList(
    parentId: Tristate[Long],
    userId: Option[Long],
    name: Option[String],
    levels: Option[String]
  ) = (silhouette.SecuredAction(AllowedRole.admin) andThen ListAction).async { implicit request =>
    toResult(Ok) {
      groupService
        .list(parentId, userId, name, levels)
        .map { groups =>
          Response.List(groups) { group =>
            ApiGroup(group)
          }
        }
    }
  }

  /**
    * Creates group and returns its model.
    */
  def create = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialGroup]) { implicit request =>
    toResult(Created) {
      val group = request.body.toModel(0)
      groupService
        .create(group)
        .map(ApiGroup(_))
    }
  }

  /**
    * Updates group and returns its model.
    */
  def update(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialGroup]) {
    implicit request =>
      toResult(Ok) {
        val draft = request.body.toModel(id)
        groupService
          .update(draft)
          .map(ApiGroup(_))
      }
  }

  /**
    * Removes group.
    */
  def delete(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async {
    groupService
      .delete(id)
      .fold(
        error => toResult(error),
        _ => NoContent
      )
  }
}
