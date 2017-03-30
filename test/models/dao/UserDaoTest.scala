package models.dao

import utils.fixture.{UserFixture, UserLoginFixture}
import utils.generator.UserGenerator
import org.scalacheck.Gen
import models.user.{User => UserModel}

class UserDaoTest extends BaseDaoTest with UserFixture with UserLoginFixture with UserGenerator {

  private val dao = inject[User]

  "get" should {
    "return users by specific criteria" in {
      forAll(Gen.option(Gen.choose(-1L, 5L))) { (id: Option[Long]) =>
        val users = wait(dao.get(id))
        users must contain theSameElementsAs Users.filter(u => id.forall(_ == u.id))
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
          val createdUserId = wait(dao.create(u, provId, provKey))
          val userById = wait(dao.findById(createdUserId))
          val userByProvider = wait(dao.findByProvider(provId, provKey))

          userById mustBe defined
          userById.get mustBe u.copy(id = createdUserId)

          userByProvider mustBe defined
          userByProvider.get mustBe u.copy(id = createdUserId)
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
}
