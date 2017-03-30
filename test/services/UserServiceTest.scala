package services

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import utils.fixture.UserFixture
import models.dao.{User => UserDAO}
import models.user.{Role, Status, User => UserModel}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import utils.generator.{SocialProfileGenerator, UserGenerator}

/**
  * Test for user service.
  */
class UserServiceTest extends BaseServiceTest with UserGenerator with SocialProfileGenerator with UserFixture {

  private val userDaoMock = mock[UserDAO]
  private val service = new User(userDaoMock)

  "retrieve" should {
    "return user by provider info" in {
      forAll { (provId: String, provKey: String, user: Option[UserModel]) =>
        when(userDaoMock.findByProvider(provId, provKey)).thenReturn(toFuture(user))

        val retrievedUser = wait(service.retrieve(LoginInfo(provId, provKey)))

        retrievedUser mustBe user
        verify(userDaoMock, times(1)).findByProvider(provId, provKey)
        verifyNoMoreInteractions(userDaoMock)
      }
    }
  }

  "createIfNotExist" should {
    "check user for existing every time" in {
      forAll { (profile: CommonSocialProfile, user: Option[UserModel]) =>
        when(userDaoMock.findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey)).thenReturn(toFuture(user))
        when(userDaoMock.create(any[UserModel], any[String], any[String])).thenReturn(toFuture(1L))

        wait(service.createIfNotExist(profile))

        verify(userDaoMock, times(1)).findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey)
      }
    }

    "does nothing if user exists" in {
      forAll { (profile: CommonSocialProfile) =>
        when(userDaoMock.findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey))
          .thenReturn(toFuture(Some(Users.head)))

        wait(service.createIfNotExist(profile))

        verify(userDaoMock, times(1)).findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey)
        verifyNoMoreInteractions(userDaoMock)
      }
    }

    "create user if not exist" in {
      forAll { (profile: CommonSocialProfile, user: Option[UserModel]) =>
        when(userDaoMock.findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey))
          .thenReturn(toFuture(None))
        when(userDaoMock.create(any[UserModel], eqTo(profile.loginInfo.providerID), eqTo(profile.loginInfo.providerKey)))
          .thenReturn(toFuture(1L))

        wait(service.createIfNotExist(profile))
        verify(userDaoMock, times(1)).findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey)

        val user = UserModel(
          0,
          profile.fullName,
          profile.email,
          Role.User,
          Status.New
        )
        verify(userDaoMock, times(1)).create(user, profile.loginInfo.providerID, profile.loginInfo.providerKey)
        verifyNoMoreInteractions(userDaoMock)
      }
    }
  }
}
