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

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import models.dao.{GroupDao, UserDao, UserGroupDao}
import models.group.Group
import models.user.{User => UserModel}
import org.davidbild.tristate.Tristate
import services.authorization.UserSda
import silhouette.CustomSocialProfile
import utils.Logger
import utils.errors.{AuthorizationError, ConflictError, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
  * User service.
  */
@Singleton
class UserService @Inject() (
  protected val userDao: UserDao,
  protected val userGroupDao: UserGroupDao,
  protected val groupDao: GroupDao,
  protected val mailService: MailService,
  protected val templateEngineService: TemplateEngineService,
  implicit val ec: ExecutionContext
) extends IdentityService[UserModel]
  with ServiceResults[UserModel]
  with Logger {

  override def retrieve(loginInfo: LoginInfo): Future[Option[UserModel]] =
    userDao.findByProvider(loginInfo.providerID, loginInfo.providerKey)

  /**
    * Creates new user, if doesn't exist. If no users exist, the user will be created as an admin.
    *
    * @param socialProfile silhouette social profile
    */
  def createIfNotExist(socialProfile: CustomSocialProfile): Future[Unit] = {

    val loginInfo = socialProfile.loginInfo

    def createNewUser(): Future[Unit] = {
      val baseNewUser = UserModel.fromSocialProfile(socialProfile)
      for {
        newUser <- userDao.count().map {
          case 0 =>
            baseNewUser.copy(
              role = UserModel.Role.Admin,
              status = UserModel.Status.Approved
            )
          case _ => baseNewUser
        }
        _ <- userDao.create(newUser, loginInfo.providerID, loginInfo.providerKey)
      } yield ()
    }

    retrieve(loginInfo).flatMap {
      case Some(_) => ().toFuture
      case None    => createNewUser()
    }
  }

  /**
    * Returns user by id.
    *
    * @param id      user ID
    * @return
    */
  def getById(id: Long): SingleResult = {
    for {
      user <- userDao.findById(id).liftRight {
        NotFoundError.User(id)
      }

    } yield user
  }

  /**
    * Returns user by id with authorization checks.
    */
  def getByIdWithAuth(id: Long)(implicit account: UserModel): SingleResult = {
    for {
      user <- getById(id)
      _ <- ensure(account.role == UserModel.Role.Admin || user.status == UserModel.Status.Approved || account.id == id) {
        AuthorizationError.General
      }
    } yield user
  }

  /**
    * Returns users list.
    *
    * @param role    role filter
    * @param status  status filter
    * @param groupId only users of the group
    * @param name    part of user name
    * @param meta    list meta
    */
  def list(
    role: Option[UserModel.Role],
    status: Option[UserModel.Status],
    groupId: Tristate[Long],
    name: Option[String]
  )(implicit meta: ListMeta): ListResult = {
    userDao
      .getList(
        optIds = None,
        optRole = role,
        optStatus = status,
        optGroupIds = groupId.map(Seq(_)),
        optName = name
      )
      .lift
  }

  /**
    * Returns users list by group ID including users of all child groups.
    */
  def listByGroupId(groupId: Long, includeDeleted: Boolean)(implicit meta: ListMeta = ListMeta.default): ListResult = {
    for {
      childGroups <- groupDao.findChildrenIds(groupId).lift
      allGroups = childGroups :+ groupId
      result <- userDao.getList(optGroupIds = Tristate.Present(allGroups), includeDeleted = includeDeleted).lift
    } yield result
  }

  /**
    * Updates user.
    *
    * @param draft   updated user draft
    * @param account logged in user
    * @return updated user
    */
  def update(
    draft: UserModel,
    updatePicture: Boolean = false
  )(implicit account: UserModel): SingleResult = {
    for {
      original <- getById(draft.id)

      _ <- UserSda.canUpdate(original, draft).liftLeft

      withoutPicture = if (updatePicture) draft else draft.copy(pictureName = original.pictureName)

      updated <- userDao.update(withoutPicture).lift
    } yield {
      if (original.status == UserModel.Status.New && updated.status == UserModel.Status.Approved) {
        sendAppprovalEmail(updated)
      }
      updated
    }
  }

  /**
    * Sends email to user when user is approved.
    */
  private def sendAppprovalEmail(user: UserModel) = {
    try {
      val bodyTemplate = templateEngineService.loadStaticTemplate("user_approved.html")
      val subject = "Open360 information"
      val context = templateEngineService.getContext(user, None)
      val body = templateEngineService.render(bodyTemplate, context)
      mailService.send(subject, user, body)
    } catch {
      case NonFatal(e) => log.error("Can't send email", e)
    }
  }

  /**
    * Deletes user.
    *
    * @param id      user id
    */
  def delete(id: Long): UnitResult = {

    def getConflictedEntities = {
      for {
        groups <- groupDao.getList(optUserId = Some(id))
      } yield {
        ConflictError.getConflictedEntitiesMap(Group.namePlural -> groups.data.map(_.toNamedEntity))
      }
    }

    for {
      _ <- getById(id)

      conflictedEntities <- getConflictedEntities.lift
      _ <- ensure(conflictedEntities.isEmpty) {
        ConflictError.General(Some(UserModel.nameSingular), conflictedEntities)
      }

      _ <- userDao.delete(id).lift
    } yield ()
  }

  /**
    * Returns group to users map.
    *
    * @param groupIds IDS of the groups to get users for
    */
  def getGroupIdToUsersMap(groupIds: Seq[Long], includeDeleted: Boolean): Future[Map[Long, Seq[UserModel]]] = {
    Future
      .sequence {
        groupIds.map { groupId =>
          listByGroupId(groupId, includeDeleted)
            .map(_.data)
            .run
            .map { maybeUsers =>
              val users = maybeUsers.getOrElse(Nil)
              (groupId, users)
            }
        }
      }
      .map(_.toMap)
  }
}
