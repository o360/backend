package models.dao

import models.user.{User => UserModel}
import org.davidbild.tristate.Tristate
import org.scalacheck.Gen
import testutils.fixture.{UserFixture, UserGroupFixture, UserLoginFixture}
import testutils.generator.{TristateGenerator, UserGenerator}

class UserDaoTest
  extends BaseDaoTest
    with UserFixture
    with UserLoginFixture
    with UserGroupFixture
    with UserGenerator
    with TristateGenerator {

  private val dao = inject[UserDao]

  "get" should {
    "return users by specific criteria" in {
      forAll(
        Gen.option(Gen.choose(-1L, 5L)),
        Gen.option(roleArbitrary.arbitrary),
        Gen.option(statusArbitrary.arbitrary)) { (
      id: Option[Long],
      role: Option[UserModel.Role],
      status: Option[UserModel.Status]
      ) =>
        val users = wait(dao.getList(id, role, status))
        val expectedUsers =
          Users.filter(u => id.forall(_ == u.id) && role.forall(_ == u.role) && status.forall(_ == u.status))
        users.total mustBe expectedUsers.length
        users.data must contain theSameElementsAs expectedUsers
      }
    }

    "return users filtered by group" in {
      forAll { (groupId: Tristate[Long]) =>
        val users = wait(dao.getList(optGroupId = groupId))
        val expectedUsers = Users.filter(u => groupId match {
          case Tristate.Unspecified => true
          case Tristate.Absent => !UserGroups.map(_._1).contains(u.id)
          case Tristate.Present(gid) => UserGroups.filter(_._2 == gid).map(_._1).contains(u.id)
        })

        users.data must contain theSameElementsAs expectedUsers
      }
    }
  }

  "findById" should {
    "return user by id" in {
      forAll(Gen.choose(-1L, 5L)) { (id: Long) =>
        val user = wait(dao.findById(id))
        user mustBe Users.find(_.id == id)
      }
    }
  }

  "findByProvider" should {
    "return user by provider data" in {
      forAll(
        Gen.choose(-1L, 5L),
        Gen.oneOf(UserLogins.map(_._2)),
        Gen.oneOf(UserLogins.map(_._3))
      ) { (userId: Long, provId: String, provKey: String) =>
        val user = wait(dao.findByProvider(provId, provKey))
        val existedUserLogin = UserLogins.find(ul => ul._2 == provId && ul._3 == provKey)
        user.map(_.id) mustBe existedUserLogin.map(_._1)
      }
    }
  }

  "create" should {
    "create user" in {
      forAll() { (u: UserModel, provId: String, provKey: String) =>
        whenever(wait(dao.findByProvider(provId, provKey)).isEmpty) {
          val createdUser = wait(dao.create(u, provId, provKey))
          val userById = wait(dao.findById(createdUser.id))
          val userByProvider = wait(dao.findByProvider(provId, provKey))

          userById mustBe defined
          userById.get mustBe createdUser

          userByProvider mustBe defined
          userByProvider.get mustBe createdUser
        }
      }
    }

    "throw exception if try to create user with existing provider key" in {
      val user = Users.head
      val provId = "newId"
      val provKey = "newKey"
      wait(dao.create(user, provId, provKey))
      an[Throwable] must be thrownBy {
        wait(dao.create(user, provId, provKey))
      }
    }
  }

  "update" should {
    "update user" in {
      val newUserId = wait(dao.create(Users(0), "updatereallyunique", "key")).id
      forAll { (user: UserModel) =>
        val userWithId = user.copy(id = newUserId)
        wait(dao.update(userWithId))
        val updatedUser = wait(dao.findById(newUserId))

        updatedUser mustBe Some(userWithId)
      }
    }
  }

  "delete" should {
    "delete user" in {
      forAll { (user: UserModel) =>
        val newUserId = wait(dao.create(user, "deletereallyunique", "key")).id
        val rowsAffected = wait(dao.delete(newUserId))
        val deletedUser = wait(dao.findById(newUserId))

        rowsAffected mustBe 1
        deletedUser mustBe empty
      }
    }
  }
}
