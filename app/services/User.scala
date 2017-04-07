package services

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.dao.{User => UserDao}
import models.user.{User => UserModel}
import services.authorization.UserAuthorization
import utils.errors.NotFoundError
import utils.listmeta.ListMeta

import scala.concurrent.Future
import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * User service.
  */
@Singleton
class User @Inject()(
  protected val userDao: UserDao
) extends IdentityService[UserModel]
  with ServiceResults[UserModel] {

  override def retrieve(loginInfo: LoginInfo): Future[Option[UserModel]] =
    userDao.findByProvider(loginInfo.providerID, loginInfo.providerKey)

  /**
    * Creates new user, if doesn't exist.
    *
    * @param socialProfile silhouette social profile
    */
  def createIfNotExist(socialProfile: CommonSocialProfile): Future[Unit] = async {
    val loginInfo = socialProfile.loginInfo
    val existedUser = await(retrieve(loginInfo))
    existedUser match {
      case Some(_) => ()
      case None =>
        val newUser = UserModel(
          0,
          socialProfile.fullName,
          socialProfile.email,
          UserModel.Role.User,
          UserModel.Status.New
        )
        await(userDao.create(newUser, loginInfo.providerID, loginInfo.providerKey))
    }
  }

  /**
    * Returns user by id.
    *
    * @param id      user ID
    * @param account logged in user
    * @return
    */
  def getById(id: Long)(implicit account: UserModel): SingleResult = async {
    UserAuthorization.canGetById(id) match {
      case Some(error) => error
      case None =>
        await(userDao.findById(id)) match {
          case None => NotFoundError.User(id)
          case Some(user) => user
        }
    }
  }

  /**
    * Returns users list.
    *
    * @param role    role filter
    * @param status  status filter
    * @param account logged in user
    * @param meta    list meta
    */
  def list(
    role: Option[UserModel.Role],
    status: Option[UserModel.Status]
  )(implicit account: UserModel, meta: ListMeta): ListResult = async {
    val users = await(userDao.get(
      role = role,
      status = status
    ))
    users
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
  )(implicit account: UserModel): SingleResult = async {
    await(getById(draft.id)) match {
      case Left(error) => error
      case Right(original) =>
        UserAuthorization.canUpdate(original, draft) match {
          case Some(error) => error
          case None =>
            await(userDao.update(draft))
            draft
        }
    }
  }

  /**
    * Deletes user.
    *
    * @param id user id
    * @param account logged in user
    */
  def delete(id: Long)(implicit account: UserModel): NoResult = async {
    await(getById(id)) match {
      case Left(error) => error
      case Right(original) =>
        await(userDao.delete(id))
        noResult
    }
  }
}
