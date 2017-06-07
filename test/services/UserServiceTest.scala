package services

import com.mohiva.play.silhouette.api.LoginInfo
import models.ListWithTotal
import models.dao.{GroupDao, UserGroupDao, UserDao => UserDAO}
import models.group.Group
import models.user.{User => UserModel}
import org.davidbild.tristate.Tristate
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import silhouette.CustomSocialProfile
import testutils.fixture.{GroupFixture, UserFixture}
import testutils.generator.{SocialProfileGenerator, TristateGenerator, UserGenerator}
import utils.errors.{AuthorizationError, ConflictError, NotFoundError}
import utils.listmeta.ListMeta

import scala.concurrent.Future

/**
  * Test for user service.
  */
class UserServiceTest
  extends BaseServiceTest
    with UserGenerator
    with SocialProfileGenerator
    with UserFixture
    with TristateGenerator
    with GroupFixture {

  private val admin = UserFixture.admin

  private case class TestFixture(
    userDaoMock: UserDAO,
    userGroupDaoMock: UserGroupDao,
    groupDaoMock: GroupDao,
    mailService: MailService,
    templateEngineService: TemplateEngineService,
    service: UserService)

  private def getFixture = {
    val daoMock = mock[UserDAO]
    val userGroupDaoMock = mock[UserGroupDao]
    val groupDaoMock = mock[GroupDao]
    val mailService = mock[MailService]
    val templateEngineService = mock[TemplateEngineService]
    val service = new UserService(daoMock, userGroupDaoMock, groupDaoMock, mailService, templateEngineService)
    TestFixture(daoMock, userGroupDaoMock, groupDaoMock, mailService, templateEngineService, service)
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
      forAll { (profile: CustomSocialProfile, user: Option[UserModel]) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey))
          .thenReturn(toFuture(user))
        when(fixture.userDaoMock.create(any[UserModel], any[String], any[String])).thenReturn(toFuture(Users(0)))

        wait(fixture.service.createIfNotExist(profile))

        verify(fixture.userDaoMock, times(1)).findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey)
      }
    }

    "does nothing if user exists" in {
      forAll { (profile: CustomSocialProfile) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey))
          .thenReturn(toFuture(Some(Users.head)))

        wait(fixture.service.createIfNotExist(profile))

        verify(fixture.userDaoMock, times(1)).findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey)
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }

    "create user if not exist" in {
      forAll { (profile: CustomSocialProfile, user: Option[UserModel]) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey))
          .thenReturn(toFuture(None))
        when(
          fixture.userDaoMock.create(
            any[UserModel],
            eqTo(profile.loginInfo.providerID),
            eqTo(profile.loginInfo.providerKey)))
          .thenReturn(toFuture(Users(0)))

        wait(fixture.service.createIfNotExist(profile))
        verify(fixture.userDaoMock, times(1)).findByProvider(profile.loginInfo.providerID, profile.loginInfo.providerKey)

        val user = UserModel.fromSocialProfile(profile)

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
          val result = wait(fixture.service.getById(id)(loggedInUser).run)

          result mustBe 'isLeft
          result.swap.toOption.get mustBe an[AuthorizationError]
        }
      }
    }

    "return not found if user not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.getById(id)(admin).run)

        result mustBe 'isLeft
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.userDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }

    "return user from db" in {
      forAll { (user: UserModel, id: Long) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findById(id)).thenReturn(toFuture(Some(user)))
        val result = wait(fixture.service.getById(id)(admin).run)

        result mustBe 'isRight
        result.toOption.get mustBe user

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
      groupId: Tristate[Long],
      name: Option[String],
      users: Seq[UserModel],
      total: Int
      ) =>
        val fixture = getFixture
        when(fixture.userDaoMock.getList(
          optId = any[Option[Long]],
          optRole = eqTo(role),
          optStatus = eqTo(status),
          optGroupIds = eqTo(groupId.map(Seq(_))),
          optName = eqTo(name),
          includeDeleted = any[Boolean]
        )(eqTo(ListMeta.default)))
          .thenReturn(toFuture(ListWithTotal(total, users)))
        val result = wait(fixture.service.list(role, status, groupId, name)(admin, ListMeta.default).run)

        result mustBe 'isRight
        result.toOption.get mustBe ListWithTotal(total, users)
      }
    }
  }

  "listByGroupId" should {
    "return list of users in group including child groups" in {
      forAll { (
      groupId: Long,
      childGroups: Seq[Long],
      includeDeleted: Boolean,
      users: Seq[UserModel],
      total: Int
      ) =>
        val fixture = getFixture
        when(fixture.groupDaoMock.findChildrenIds(groupId)).thenReturn(toFuture(childGroups))
        when(fixture.userDaoMock.getList(
          optId = any[Option[Long]],
          optRole = any[Option[UserModel.Role]],
          optStatus = any[Option[UserModel.Status]],
          optGroupIds = eqTo(Tristate.Present(childGroups :+ groupId)),
          optName = any[Option[String]],
          includeDeleted = eqTo(includeDeleted)
        )(eqTo(ListMeta.default))).thenReturn(toFuture(ListWithTotal(total, users)))

        val result = wait(fixture.service.listByGroupId(groupId, includeDeleted).run)

        result mustBe 'isRight
        result.toOption.get mustBe ListWithTotal(total, users)
      }
    }
  }

  "update" should {
    "return not found if user not found" in {
      forAll { (user: UserModel) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findById(user.id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.update(user)(admin).run)

        result mustBe 'isLeft
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.userDaoMock, times(1)).findById(user.id)
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }

    "return error if authorization failed" in {
      forAll { (loggedInUser: UserModel) =>
        val fixture = getFixture
        whenever(loggedInUser.role != UserModel.Role.Admin) {
          when(fixture.userDaoMock.findById(loggedInUser.id)).thenReturn(toFuture(Some(loggedInUser)))
          val result = wait(fixture.service.update(loggedInUser.copy(role = UserModel.Role.Admin))(loggedInUser).run)

          result mustBe 'isLeft
          result.swap.toOption.get mustBe an[AuthorizationError]

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
          gender = Some(UserModel.Gender.Male),
          role = UserModel.Role.Admin,
          status = UserModel.Status.Approved
        )
        val template = "template"
        val renderedTemplate = "rendered template"
        when(fixture.userDaoMock.findById(user.id)).thenReturn(Future.successful(Some(user)))
        when(fixture.userDaoMock.update(updatedUser)).thenReturn(toFuture(updatedUser))
        when(fixture.templateEngineService.loadStaticTemplate("user_approved.html")).thenReturn(template)
        when(fixture.templateEngineService.getContext(updatedUser, None)).thenReturn(Map("a" -> "b"))
        when(fixture.templateEngineService.render(template, Map("a" -> "b"))).thenReturn(renderedTemplate)

        val result = wait(fixture.service.update(updatedUser)(admin).run)

        result mustBe 'isRight
        result.toOption.get mustBe updatedUser

        verify(fixture.userDaoMock, times(1)).findById(user.id)
        verify(fixture.userDaoMock, times(1)).update(updatedUser)
        if (user.status == UserModel.Status.New) {
          verify(fixture.mailService, times(1)).send("Assessment system information", updatedUser, renderedTemplate)
        }
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }
  }

  "delete" should {
    "return not found if user not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.delete(id)(admin).run)

        result mustBe 'isLeft
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.userDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }
    "return conflict if user is in any group" in {
      forAll { (user: UserModel, id: Long) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findById(id)).thenReturn(toFuture(Some(user.copy(id = id))))
        when(fixture.groupDaoMock.getList(
          optId = any[Option[Long]],
          optParentId = any[Tristate[Long]],
          optUserId = eqTo(Some(id))
        )(any[ListMeta])).thenReturn(toFuture(ListWithTotal(1, Groups.take(1))))
        val result = wait(fixture.service.delete(id)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[ConflictError]
      }
    }

    "delete user from db" in {
      forAll { (user: UserModel, id: Long) =>
        val fixture = getFixture
        when(fixture.userDaoMock.findById(id)).thenReturn(toFuture(Some(user.copy(id = id))))
        when(fixture.groupDaoMock.getList(
          optId = any[Option[Long]],
          optParentId = any[Tristate[Long]],
          optUserId = eqTo(Some(id))
        )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Group](0, Nil)))
        when(fixture.userDaoMock.delete(id)).thenReturn(toFuture(()))
        val result = wait(fixture.service.delete(id)(admin).run)

        result mustBe 'isRight

        verify(fixture.userDaoMock, times(1)).findById(id)
        verify(fixture.userDaoMock, times(1)).delete(id)
        verifyNoMoreInteractions(fixture.userDaoMock)
      }
    }
  }

  "getGroupIdToUserMap" should {
    "return group-users map" in {
      val fixture = getFixture

      val groupIds = Seq(1L, 2L)
      val firstGroupChild = Seq(3L, 4L)
      val secondGroupChild = Seq(5L, 6L)

      val usersOfFirstGroup = Seq(Users(0), Users(1))
      val usersOfSecondGroup = Seq(Users(1), Users(2))
      val includeDeleted = true

      when(fixture.groupDaoMock.findChildrenIds(1))
        .thenReturn(toFuture(firstGroupChild))
      when(fixture.groupDaoMock.findChildrenIds(2))
        .thenReturn(toFuture(secondGroupChild))

      when(fixture.userDaoMock.getList(
        optId = any[Option[Long]],
        optRole = any[Option[UserModel.Role]],
        optStatus = any[Option[UserModel.Status]],
        optGroupIds = eqTo(Tristate.Present(firstGroupChild :+ 1L)),
        optName = any[Option[String]],
        includeDeleted = eqTo(includeDeleted)
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal(2, usersOfFirstGroup)))
      when(fixture.userDaoMock.getList(
        optId = any[Option[Long]],
        optRole = any[Option[UserModel.Role]],
        optStatus = any[Option[UserModel.Status]],
        optGroupIds = eqTo(Tristate.Present(secondGroupChild :+ 2L)),
        optName = any[Option[String]],
        includeDeleted = eqTo(includeDeleted)
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal(2, usersOfSecondGroup)))

      val result = wait(fixture.service.getGroupIdToUsersMap(groupIds, includeDeleted = true))
      val expectedResult = Map(1L -> usersOfFirstGroup, 2L -> usersOfSecondGroup)

      result mustBe expectedResult
    }
  }
}
