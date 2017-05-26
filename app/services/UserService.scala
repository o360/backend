package services

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import models.dao.{GroupDao, UserDao, UserGroupDao}
import models.user.{User => UserModel}
import org.davidbild.tristate.Tristate
import play.api.libs.concurrent.Execution.Implicits._
import services.authorization.UserSda
import silhouette.CustomSocialProfile
import utils.errors.{ConflictError, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.Future

/**
  * User service.
  */
@Singleton
class UserService @Inject()(
  protected val userDao: UserDao,
  protected val userGroupDao: UserGroupDao,
  protected val groupDao: GroupDao
) extends IdentityService[UserModel]
  with ServiceResults[UserModel] {

  override def retrieve(loginInfo: LoginInfo): Future[Option[UserModel]] =
    userDao.findByProvider(loginInfo.providerID, loginInfo.providerKey)

  /**
    * Creates new user, if doesn't exist.
    *
    * @param socialProfile silhouette social profile
    */
  def createIfNotExist(socialProfile: CustomSocialProfile): Future[Unit] = {
    val loginInfo = socialProfile.loginInfo
    retrieve(loginInfo).flatMap {
      case Some(_) => ().toFuture
      case None =>
        val newUser = UserModel.fromSocialProfile(socialProfile)
        userDao.create(newUser, loginInfo.providerID, loginInfo.providerKey).map(_ => ())
    }
  }

  /**
    * Returns user by id.
    *
    * @param id      user ID
    * @param account logged in user
    * @return
    */
  def getById(id: Long)(implicit account: UserModel): SingleResult = {
    for {
      _ <- UserSda.canGetById(id).liftLeft
      user <- userDao.findById(id).liftRight {
        NotFoundError.User(id)
      }
    } yield user
  }

  /**
    * Returns users list.
    *
    * @param role    role filter
    * @param status  status filter
    * @param account logged in user
    * @param groupId only users of the group
    * @param name    part of user name
    * @param meta    list meta
    */
  def list(
    role: Option[UserModel.Role],
    status: Option[UserModel.Status],
    groupId: Tristate[Long],
    name: Option[String]
  )(implicit account: UserModel, meta: ListMeta): ListResult = {
    userDao.getList(
      optId = None,
      optRole = role,
      optStatus = status,
      optGroupIds = groupId.map(Seq(_)),
      optName = name
    ).lift
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
    draft: UserModel
  )(implicit account: UserModel): SingleResult = {
    for {
      original <- getById(draft.id)

      _ <- UserSda.canUpdate(original, draft).liftLeft

      updated <- userDao.update(draft).lift
    } yield updated
  }

  /**
    * Deletes user.
    *
    * @param id      user id
    * @param account logged in user
    */
  def delete(id: Long)(implicit account: UserModel): UnitResult = {
    for {
      _ <- getById(id)

      _ <- ensure(!userGroupDao.exists(userId = Some(id))) {
        ConflictError.User.GroupExists(id)
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
    Future.sequence {
      groupIds.map { groupId =>
        listByGroupId(groupId, includeDeleted)
          .map(_.data)
          .run
          .map { maybeUsers =>
            val users = maybeUsers.getOrElse(Nil)
            (groupId, users)
          }
      }
    }.map(_.toMap)
  }
}
