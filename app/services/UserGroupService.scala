package services

import javax.inject.{Inject, Singleton}

import models.dao.UserGroupDao
import models.user.User
import utils.errors.ApplicationError

import scala.async.Async._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

/**
  * User group service.
  */
@Singleton
class UserGroupService @Inject()(
  protected val userService: UserService,
  protected val groupService: GroupService,
  protected val userGroupDao: UserGroupDao
) extends ServiceResults[Unit] {

  /**
    * Checks whether user and group available.
    *
    * @param groupId group ID
    * @param userId  user ID
    * @return none in case of success, some error otherwise
    */
  private def checkUserGroup(groupId: Long, userId: Long)(implicit account: User): Future[Option[ApplicationError]] = async {
    // TODO Rewrite using for-comp.
    await(userService.getById(userId)) match {
      case Left(error) => Some(error)
      case _ =>
        await(groupService.getById(groupId)) match {
          case Left(error) => Some(error)
          case _ =>
            None
        }
    }
  }

  /**
    * Adds user to group.
    *
    * @param groupId group ID
    * @param userId  user ID
    */
  def add(groupId: Long, userId: Long)(implicit account: User): UnitResult = async {
    await(checkUserGroup(groupId, userId)) match {
      case Some(error) => error
      case None =>
        if (!await(userGroupDao.exists(Some(groupId), Some(userId)))) {
          await(userGroupDao.add(groupId, userId))
        }
        unitResult
    }
  }

  /**
    * Removes user from group.
    *
    * @param groupId group ID
    * @param userId  user ID
    */
  def remove(groupId: Long, userId: Long)(implicit account: User): UnitResult = async {
    await(checkUserGroup(groupId, userId)) match {
      case Some(error) => error
      case None =>
        await(userGroupDao.remove(groupId, userId))
        unitResult
    }
  }
}
