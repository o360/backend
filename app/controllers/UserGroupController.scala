package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.api.group.ApiUserGroup
import controllers.authorization.AllowedRole
import play.api.mvc.ControllerComponents
import services.UserGroupService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._

import scala.concurrent.ExecutionContext

/**
  * User group controller.
  */
@Singleton
class UserGroupController @Inject()(
  protected val silhouette: Silhouette[DefaultEnv],
  protected val userGroupService: UserGroupService,
  val controllerComponents: ControllerComponents,
  implicit val ec: ExecutionContext
) extends BaseController {

  /**
    * Adds user to group.
    *
    * @param groupId group ID
    * @param userId  user ID
    */
  def add(groupId: Long, userId: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    userGroupService
      .add(groupId, userId)
      .fold(
        error => toResult(error),
        _ => NoContent
      )
  }

  /**
    * Removes user from group.
    *
    * @param groupId group ID
    * @param userId  user ID
    */
  def remove(groupId: Long, userId: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    userGroupService
      .remove(groupId, userId)
      .fold(
        error => toResult(error),
        _ => NoContent
      )
  }

  /**
    * Bulk adds users to groups.
    */
  def bulkAdd =
    silhouette
      .SecuredAction(AllowedRole.admin)
      .async(parse.json[Seq[ApiUserGroup]]) { implicit request =>
        userGroupService
          .bulkAdd(request.body.map(x => (x.groupId, x.userId)))
          .fold(
            error => toResult(error),
            _ => NoContent
          )
      }

  /**
    * Bulk removes users from groups.
    */
  def bulkRemove =
    silhouette
      .SecuredAction(AllowedRole.admin)
      .async(parse.json[Seq[ApiUserGroup]]) { implicit request =>
        userGroupService
          .bulkRemove(request.body.map(x => (x.groupId, x.userId)))
          .fold(
            error => toResult(error),
            _ => NoContent
          )
      }
}
