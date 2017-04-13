package controllers

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.Silhouette
import controllers.authorization.AllowedRole
import play.api.libs.concurrent.Execution.Implicits._
import services.UserGroupService
import silhouette.DefaultEnv

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
    userGroupService.add(groupId, userId).map {
      case Left(error) => toResult(error)
      case Right(_) => NoContent
    }
  }

  /**
    * Removes user from group.
    *
    * @param groupId group ID
    * @param userId  user ID
    */
  def remove(groupId: Long, userId: Long) = silhouette.SecuredAction(AllowedRole.admin).async { implicit request =>
    userGroupService.remove(groupId, userId).map {
      case Left(error) => toResult(error)
      case Right(_) => NoContent
    }
  }
}
