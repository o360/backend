package services

import java.sql.Timestamp

import models.ListWithTotal
import models.dao.{GroupDao, InviteDao, UserDao, UserGroupDao}
import models.invite.Invite
import models.user.User
import org.davidbild.tristate.Tristate
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import testutils.fixture.{GroupFixture, InviteFixture, UserFixture}
import testutils.generator.InviteGenerator
import utils.errors.{ConflictError, NotFoundError}
import utils.listmeta.ListMeta

/**
  * Test for invite service.
  */
class InviteServiceTest
  extends BaseServiceTest
  with InviteGenerator
  with InviteFixture
  with GroupFixture
  with UserFixture {

  private case class Fixture(
    userDao: UserDao,
    groupDao: GroupDao,
    inviteDao: InviteDao,
    userGroupDao: UserGroupDao,
    mailService: MailService,
    templateEngineService: TemplateEngineService,
    service: InviteService
  )

  private def getFixture = {
    val userDaoMock = mock[UserDao]
    val groupDaoMock = mock[GroupDao]
    val inviteDaoMock = mock[InviteDao]
    val userGroupDaoMock = mock[UserGroupDao]
    val mailServiceMock = mock[MailService]
    val templateEngineServiceMock = mock[TemplateEngineService]
    val service = new InviteService(userDaoMock,
                                    groupDaoMock,
                                    inviteDaoMock,
                                    userGroupDaoMock,
                                    mailServiceMock,
                                    templateEngineServiceMock)
    Fixture(userDaoMock,
            groupDaoMock,
            inviteDaoMock,
            userGroupDaoMock,
            mailServiceMock,
            templateEngineServiceMock,
            service)
  }

  "getList" should {
    "return list" in {
      forAll { (invites: Seq[Invite], total: Int) =>
        val fixture = getFixture

        when(fixture.inviteDao.getList(any[ListMeta])).thenReturn(toFuture(ListWithTotal(total, invites)))
        val result = wait(fixture.service.getList().run)

        result.toOption.get mustBe ListWithTotal(total, invites)
      }
    }
  }

  "createInvites" should {
    "return error if validation failed" in {
      val fixture = getFixture
      val email = "someemail"
      val groupId = 1
      when(
        fixture.userDao.getList(
          optIds = any[Option[Seq[Long]]],
          optRole = any[Option[User.Role]],
          optStatus = any[Option[User.Status]],
          optGroupIds = any[Tristate[Seq[Long]]],
          optName = any[Option[String]],
          optEmail = eqTo(Some(email)),
          includeDeleted = any[Boolean]
        )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[User](0, Nil)))
      when(fixture.groupDao.findById(groupId)).thenReturn(toFuture(None))

      val result = wait(fixture.service.createInvites(Seq(Invite(email, Set(groupId)))).run)

      result mustBe 'isLeft
      result.swap.toOption.get mustBe a[NotFoundError]
    }

    "create invites" in {
      val fixture = getFixture
      val email = "someemail"
      val groupId = 1
      val invite = Invite(email, Set(groupId))

      when(
        fixture.userDao.getList(
          optIds = any[Option[Seq[Long]]],
          optRole = any[Option[User.Role]],
          optStatus = any[Option[User.Status]],
          optGroupIds = any[Tristate[Seq[Long]]],
          optName = any[Option[String]],
          optEmail = eqTo(Some(email)),
          includeDeleted = any[Boolean]
        )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[User](0, Nil)))
      when(fixture.groupDao.findById(groupId)).thenReturn(toFuture(Some(Groups(0))))
      when(fixture.inviteDao.create(any[Invite])).thenReturn(toFuture(invite))
      when(fixture.templateEngineService.loadStaticTemplate("user_invited.html")).thenReturn("template")
      when(fixture.templateEngineService.render(eqTo("template"), any[Map[String, String]])).thenReturn("rendered")

      val result = wait(fixture.service.createInvites(Seq(invite)).run)

      result mustBe 'isRight
    }
  }

  "applyInvite" should {
    "return not found if code not found" in {
      val fixture = getFixture
      val code = "code"

      when(fixture.inviteDao.findByCode(code)).thenReturn(toFuture(None))

      val result = wait(fixture.service.applyInvite(code)(Users(2)).run)

      result mustBe 'isLeft
      result.swap.toOption.get mustBe a[NotFoundError]
    }

    "return conflict if code already activated" in {
      val fixture = getFixture
      val code = "code"

      val invite = Invite(code, "email", Set(), Some(new Timestamp(0)), new Timestamp(0))
      when(fixture.inviteDao.findByCode(code)).thenReturn(toFuture(Some(invite)))

      val result = wait(fixture.service.applyInvite(code)(Users(2)).run)

      result mustBe 'isLeft
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "return conflict if user already approved" in {
      val fixture = getFixture
      val code = "code"

      val invite = Invite(code, "email", Set(), None, new Timestamp(0))
      when(fixture.inviteDao.findByCode(code)).thenReturn(toFuture(Some(invite)))

      val result = wait(fixture.service.applyInvite(code)(Users(0)).run)

      result mustBe 'isLeft
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "approve user and add him to groups" in {
      val fixture = getFixture
      val code = "code"
      val groupId = 1

      val invite = Invite(code, "email", Set(groupId), None, new Timestamp(0))
      when(fixture.inviteDao.findByCode(code)).thenReturn(toFuture(Some(invite)))
      when(fixture.userDao.update(Users(2).copy(status = User.Status.Approved))).thenReturn(toFuture(Users(2)))
      when(fixture.userGroupDao.add(groupId, Users(2).id)).thenReturn(toFuture(()))
      when(fixture.inviteDao.activate(eqTo(code), any[Timestamp])).thenReturn(toFuture(()))

      val result = wait(fixture.service.applyInvite(code)(Users(2)).run)

      result mustBe 'isRight
    }
  }
}
