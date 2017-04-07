package services

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.ListWithTotal
import models.dao.{User => UserDAO}
import models.user.{User => UserModel}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import testutils.fixture.UserFixture
import testutils.generator.{SocialProfileGenerator, UserGenerator}
import utils.errors.{AuthorizationError, NotFoundError}
import utils.listmeta.ListMeta

import scala.concurrent.Future

/**
  * Test for user service.
  */
class UserServiceTest extends BaseServiceTest with UserGenerator with SocialProfileGenerator with UserFixture {

  private val admin = UserModel(1, None, None, UserModel.Role.Admin, UserModel.Status.Approved)

  private case class TestFixture(userDaoMock: UserDAO, service: User)

  private def getFixture = {
    val daoMock = mock[UserDAO]
    val service = new User(daoMock)
    TestFixture(daoMock, service)
  }


  "retrieve" should {
    "return user by provider info" in {
      forAll { (provId: String, provKey: String, user: Option[UserModel]) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findByProvider(provId, provKey)).thenReturn(toFuture(user))

        val retrievedUser = wait(fixture.service.retrieve(LoginInfo(provId, provKey)))

        retrievedUser mustBe user
        verify(fixture.userDaoMock, times(1)).findByProvider(provId, provKey)
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }
  }

  "createIfNotExist" should {
    "check user for existing every time" in {
      forAll { (profile: CommonSocialProfile, user: Option[UserModel]) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey))
          .thenReturn(toFuture(user))
        when(fixture.userDaoMock.create(any[UserModel], any[String], any[String])).thenReturn(toFuture(1L))

        wait(fixture.service.createIfNotExist(profile))

        verify(fixture.userDaoMock, times(1)).findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey)
      }
    }

    "does nothing if user exists" in {
      forAll { (profile: CommonSocialProfile) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey))
          .thenReturn(toFuture(Some(Users.head)))

        wait(fixture.service.createIfNotExist(profile))

        verify(fixture.userDaoMock, times(1)).findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey)
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }

    "create user if not exist" in {
      forAll { (profile: CommonSocialProfile, user: Option[UserModel]) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey))
          .thenReturn(toFuture(None))
        when(
          fixture.userDaoMock.create(
            any[UserModel],
            eqTo(profile.loginInfo.providerID),
            eqTo(profile.loginInfo.providerKey)))
          .thenReturn(toFuture(1L))

        wait(fixture.service.createIfNotExist(profile))
        verify(fixture.userDaoMock, times(1)).findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey)

        val user = UserModel(
          0,
          profile.fullName,
          profile.email,
          UserModel.Role.User,
          UserModel.Status.New
        )
        verify(fixture.userDaoMock, times(1)).create(user, profile.loginInfo.providerID, profile.loginInfo.providerKey)
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }
  }

  "getById" should {
    "return error if authorization failed" in {
      forAll { (loggedInUser: UserModel, id: Long) =>
        val fixture = getFixture
        whenever(loggedInUser.id != id && loggedInUser.role != UserModel.Role.Admin) {
          val result = wait(fixture.service.getById(id)(loggedInUser))

          result mustBe 'isLeft
          result.left.get mustBe an[AuthorizationError]
        }
      }
    }

    "return not found if user not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.getById(id)(admin))

        result mustBe 'isLeft
        result.left.get mustBe a[NotFoundError]

        verify(fixture.userDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }

    "return user from db" in {
      forAll { (user: UserModel, id: Long) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findById(id)).thenReturn(toFuture(Some(user)))
        val result = wait(fixture.service.getById(id)(admin))

        result mustBe 'isRight
        result.right.get mustBe user

        verify(fixture.userDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }
  }

  "list" should {
    "return list of users from db" in {
      forAll { (
      role: Option[UserModel.Role],
      status: Option[UserModel.Status],
      users: Seq[UserModel],
      total: Int
      ) =>
        val fixture = getFixture
        when(fixture.userDaoMock.get(
          id = any[Option[Long]],
          role = eqTo(role),
          status = eqTo(status)
        )(eqTo(ListMeta.default)))
          .thenReturn(toFuture(ListWithTotal(total, users)))
        val result = wait(fixture.service.list(role, status)(admin, ListMeta.default))

        result mustBe 'isRight
        result.right.get mustBe ListWithTotal(total, users)

        verify(fixture.userDaoMock, times(1)).get(
          id = any[Option[Long]],
          role = eqTo(role),
          status = eqTo(status)
        )(eqTo(ListMeta.default))
      }
    }
  }

  "update" should {
    "return not found if user not found" in {
      forAll { (user: UserModel) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findById(user.id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.update(user)(admin))

        result mustBe 'isLeft
        result.left.get mustBe a[NotFoundError]

        verify(fixture.userDaoMock, times(1)).findById(user.id)
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }

    "return error if authorization failed" in {
      forAll { (loggedInUser: UserModel) =>
        val fixture = getFixture
        whenever(loggedInUser.role != UserModel.Role.Admin) {
          when(fixture.userDaoMock.findById(loggedInUser.id)).thenReturn(toFuture(Some(loggedInUser)))
          val result = wait(fixture.service.update(loggedInUser.copy(role = UserModel.Role.Admin))(loggedInUser))

          result mustBe 'isLeft
          result.left.get mustBe an[AuthorizationError]

          verify(fixture.userDaoMock, times(1)).findById(loggedInUser.id)
          verifyNoMoreInteractions(fixture.userDaoMock)
        }
      }
    }

    "update user in db" in {
      forAll { (user: UserModel) =>
        val fixture = getFixture
        val updatedUser = user.copy(
          name = Some("name"),
          email = Some("email"),
          role = UserModel.Role.Admin,
          status = UserModel.Status.Approved
        )
        when(fixture.userDaoMock.findById(user.id)).thenReturn(Future.successful(Some(user)))
        when(fixture.userDaoMock.update(updatedUser)).thenReturn(toFuture(1))

        val result = wait(fixture.service.update(updatedUser)(admin))

        result mustBe 'isRight
        result.right.get mustBe updatedUser

        verify(fixture.userDaoMock, times(1)).findById(user.id)
        verify(fixture.userDaoMock, times(1)).update(updatedUser)
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }
  }

  "delete" should {
    "return not found if user not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.delete(id)(admin))

        result mustBe 'isLeft
        result.left.get mustBe a[NotFoundError]

        verify(fixture.userDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }
    "delete user from db" in {
      forAll { (user: UserModel, id: Long) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findById(id)).thenReturn(toFuture(Some(user.copy(id = id))))
        when(fixture.userDaoMock.delete(id)).thenReturn(toFuture(1))
        val result = wait(fixture.service.delete(id)(admin))

        result mustBe 'isRight

        verify(fixture.userDaoMock, times(1)).findById(id)
        verify(fixture.userDaoMock, times(1)).delete(id)
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }
  }
}
