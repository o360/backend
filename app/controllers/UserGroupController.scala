package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.authorization.AllowedRole
import services.UserGroupService
import silhouette.DefaultEnv
import utils.implicits.FutureLifting._

/**
  * User group controller.
  */
@Singleton
class UserGroupController @Inject()(
  protected val silhouette: Silhouette[DefaultEnv],
  protected val userGroupService: UserGroupService
) extends BaseController {

  /**
    * Adds user to group.
    *
    * @param groupId group ID
    * @param userId  user ID
    */
  def add(groupId: Long, userId: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    userGroupService.add(groupId, userId).fold(
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
    userGroupService.remove(groupId, userId).fold(
      error => toResult(error),
      _ => NoContent
    )
  }
}
