package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.api.Response
import controllers.api.group.{ApiGroup, ApiPartialGroup}
import controllers.authorization.AllowedRole
import org.davidbild.tristate.Tristate
import play.api.libs.concurrent.Execution.Implicits._
import services.GroupService
import silhouette.DefaultEnv
import utils.listmeta.actions.ListActions
import utils.listmeta.sorting.Sorting

import scala.async.Async._

/**
  * Group controller.
  */
@Singleton
class GroupController @Inject()(
  silhouette: Silhouette[DefaultEnv],
  groupService: GroupService
) extends BaseController with ListActions {

  implicit val sortingFields = Sorting.AvailableFields('id, 'name)

  /**
    * Returns group by ID.
    */
  def getById(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    async {
      toResult(Ok) {
        await(groupService.getById(id)).right.map(ApiGroup(_))
      }
    }
  }

  /**
    * Returns filtered groups list.
    */
  def getList(
    parentId: Tristate[Long]
  ) = (silhouette.SecuredAction(AllowedRole.admin) andThen ListAction).async { implicit request =>
    async {
      toResult(Ok) {
        val groups = await(
          groupService.list(parentId)
        )
        Response.List(groups) {
          ApiGroup(_)
        }
      }
    }
  }

  /**
    * Creates group and returns its model.
    */
  def create = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialGroup]) { implicit request =>
    async {
      toResult(Created) {
        val group = request.body.toModel(0)
        for {
          created <- await(groupService.create(group)).right
        } yield ApiGroup(created)
      }
    }
  }

  /**
    * Updates group and returns its model.
    */
  def update(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async(parse.json[ApiPartialGroup]) { implicit request =>
    async {
      toResult(Ok) {
        val draft = request.body.toModel(id)
        for {
          updated <- await(groupService.update(draft)).right
        } yield ApiGroup(updated)
      }
    }
  }

  /**
    * Removes group.
    */
  def delete(id: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    async {
      await(groupService.delete(id)) match {
        case Left(error) => toResult(error)
        case Right(_) => NoContent
      }
    }
  }
}
