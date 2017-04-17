package services

import javax.inject.{Inject, Singleton}

import models.dao.UserGroupDao
import models.user.User
import utils.implicits.FutureLifting._

import scalaz.Scalaz._


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
  private def validateUserGroup(groupId: Long, userId: Long)(implicit account: User): UnitResult = {
    for {
      _ <- userService.getById(userId)
      _ <- groupService.getById(groupId)
    } yield ()
  }

  /**
    * Adds user to group.
    *
    * @param groupId group ID
    * @param userId  user ID
    */
  def add(groupId: Long, userId: Long)(implicit account: User): UnitResult = {
    for {
      _ <- validateUserGroup(groupId, userId)

      isAlreadyExists <- userGroupDao.exists(Some(groupId), Some(userId)).lift
      _ <- { isAlreadyExists ? ().toFuture | userGroupDao.add(groupId, userId) }.lift
    } yield ()
  }

  /**
    * Removes user from group.
    *
    * @param groupId group ID
    * @param userId  user ID
    */
  def remove(groupId: Long, userId: Long)(implicit account: User): UnitResult = {
    for {
      _ <- validateUserGroup(groupId, userId)
      _ <- userGroupDao.remove(groupId, userId).lift
    } yield ()
  }
}
