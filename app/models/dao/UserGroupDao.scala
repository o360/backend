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

package models.dao

import javax.inject.{Inject, Singleton}

import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
  * Component for 'user_group' table.
  */
trait UserGroupComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  case class DbUserGroup(userId: Long, groupId: Long)

  class UserGroupTable(tag: Tag) extends Table[DbUserGroup](tag, "user_group") {

    def userId = column[Long]("user_id", O.PrimaryKey)
    def groupId = column[Long]("group_id", O.PrimaryKey)

    def * = (userId, groupId) <> ((DbUserGroup.apply _).tupled, DbUserGroup.unapply)
  }

  val UserGroups = TableQuery[UserGroupTable]
}

/**
  * DAO for user - group relation.
  */
@Singleton
class UserGroupDao @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider,
  implicit val ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
  with UserGroupComponent
  with DaoHelper {

  import profile.api._

  /**
    * Checks whether relation exists.
    *
    * @param groupId group ID
    * @param userId  user ID
    * @return true, if relation exists and fits given criteria
    */
  def exists(groupId: Option[Long] = None, userId: Option[Long] = None): Future[Boolean] = db.run {
    UserGroups
      .applyFilter { x =>
        Seq(
          userId.map(x.userId === _),
          groupId.map(x.groupId === _)
        )
      }
      .exists
      .result
  }

  /**
    * Adds user to group.
    *
    * @param groupId group ID
    * @param userId  user ID
    */
  def add(groupId: Long, userId: Long): Future[Unit] = {
    val actions = for {
      exists <- UserGroups.filter(x => x.groupId === groupId && x.userId === userId).exists.result
      _ <- if (exists) DBIO.successful(()) else UserGroups += DbUserGroup(userId, groupId)
    } yield ()
    db.run {
        actions.transactionally
      }
      .map(_ => ())
  }

  /**
    * Removes user from group.
    *
    * @param groupId group ID
    * @param userId  user ID
    */
  def remove(groupId: Long, userId: Long): Future[Unit] =
    db.run {
        UserGroups.filter(x => x.userId === userId && x.groupId === groupId).delete
      }
      .map(_ => ())
}
