package services

import javax.inject.{Inject, Singleton}

import models.dao.UserGroupDao
import models.user.User
import utils.errors.ConflictError
import utils.implicits.FutureLifting._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.-\/
import scalaz.Scalaz._

/**
  * User group service.
  */
@Singleton
class UserGroupService @Inject()(
  protected val userService: UserService,
  protected val groupService: GroupService,
  protected val userGroupDao: UserGroupDao,
  implicit val ec: ExecutionContext
) extends ServiceResults[Unit] {

  type GroupUser = (Long, Long)

  /**
    * Checks whether user and group available.
    *
    * @param groupId group ID
    * @param userId  user ID
    * @return none in case of success, some error otherwise
    */
  private def validateUserGroup(groupId: Long, userId: Long)(implicit account: User): UnitResult = {
    for {
      user <- userService.getById(userId)

      _ <- ensure(user.status == User.Status.Approved) {
        ConflictError.User.Unapproved
      }

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

  def bulkAdd(groupUsers: Seq[GroupUser])(implicit account: User): UnitResult = {
    bulkAction(
      groupUsers, {
        case (groupId, userId) =>
          userGroupDao.exists(Some(groupId), Some(userId)).flatMap { isAlreadyExists =>
            if (!isAlreadyExists) {
              userGroupDao.add(groupId, userId)
            } else ().toFuture
          }
      }
    )
  }

  def bulkRemove(groupUsers: Seq[GroupUser])(implicit account: User): UnitResult = {
    bulkAction(groupUsers, {
      case (groupId, userId) => userGroupDao.remove(groupId, userId)
    })
  }

  private def bulkAction(groupUsers: Seq[GroupUser], action: GroupUser => Future[Unit])(
    implicit account: User): UnitResult = {
    for {
      maybeErrors <- Future.sequence {
        groupUsers.map {
          case (groupId, userId) =>
            validateUserGroup(groupId, userId).run
        }
      }.lift
      maybeError = maybeErrors.collect {
        case -\/(error) => error
      }.headOption
      _ <- maybeError.liftLeft

      _ <- Future.sequence {
        groupUsers.map(action)
      }.lift
    } yield ()
  }
}
