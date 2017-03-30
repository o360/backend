package services

import javax.inject.{Inject, Singleton}

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.dao.{User => UserDao}
import models.user.{Role, Status, User => UserModel}

import scala.concurrent.Future
import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * User service.
  */
@Singleton
class User @Inject()(
  protected val userDao: UserDao
) extends IdentityService[UserModel] {

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
          Role.User,
          Status.New
        )
        await(userDao.create(newUser, loginInfo.providerID, loginInfo.providerKey))
    }
  }

}
