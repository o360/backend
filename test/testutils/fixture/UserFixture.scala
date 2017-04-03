package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.user.{Role, Status, User => UserModel}

/**
  * User's model fixture.
  */
trait UserFixture extends FixtureHelper {
  self: FixtureSupport =>
  val Users = Seq(
    UserModel(1, Some("adminname"), Some("admin@email.com"), Role.Admin, Status.Approved),
    UserModel(2, Some("username"), Some("user@email.com"), Role.User, Status.Approved)
  )

  addFixtureOperation {
    insertInto("account")
      .columns("id", "name", "email", "role", "status")
      .scalaValues("1", "adminname", "admin@email.com", "1", "1")
      .scalaValues("2", "username", "user@email.com", "0", "1")
      .build
  }

}
