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
class UserGroupService @Inject() (
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
  private def validateUserGroup(groupId: Long, userId: Long): UnitResult = {
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
  def add(groupId: Long, userId: Long): UnitResult = {
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
  def remove(groupId: Long, userId: Long): UnitResult = {
    for {
      _ <- validateUserGroup(groupId, userId)
      _ <- userGroupDao.remove(groupId, userId).lift
    } yield ()
  }

  def bulkAdd(groupUsers: Seq[GroupUser]): UnitResult = {
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

  def bulkRemove(groupUsers: Seq[GroupUser]): UnitResult = {
    bulkAction(groupUsers, {
      case (groupId, userId) => userGroupDao.remove(groupId, userId)
    })
  }

  private def bulkAction(groupUsers: Seq[GroupUser], action: GroupUser => Future[Unit]): UnitResult = {
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
