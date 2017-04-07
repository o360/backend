package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.user.{User => UserModel}

/**
  * User's model fixture.
  */
trait UserFixture extends FixtureHelper {
  self: FixtureSupport =>
  val Users = Seq(
    UserModel(1, Some("adminname"), Some("admin@email.com"), UserModel.Role.Admin, UserModel.Status.Approved),
    UserModel(2, Some("username"), Some("user@email.com"), UserModel.Role.User, UserModel.Status.Approved),
    UserModel(3, Some("newuser"), Some("newuser@email.com"), UserModel.Role.User, UserModel.Status.New)
  )

  addFixtureOperation {
    insertInto("account")
      .columns("id", "name", "email", "role", "status")
      .scalaValues("1", "adminname", "admin@email.com", "1", "1")
      .scalaValues("2", "username", "user@email.com", "0", "1")
      .scalaValues("3", "newuser", "newuser@email.com", "0", "0")
      .build
  }

}
