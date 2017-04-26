package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.user.User

/**
  * User's model fixture.
  */
trait UserFixture extends FixtureHelper {
  self: FixtureSupport =>
  val Users = Seq(
    User(1, Some("adminname"), Some("admin@email.com"), Some(User.Gender.Male), User.Role.Admin, User.Status.Approved),
    User(2, Some("username"), Some("user@email.com"), Some(User.Gender.Female), User.Role.User, User.Status.Approved),
    User(3, Some("newuser"), Some("newuser@email.com"), None, User.Role.User, User.Status.New)
  )

  addFixtureOperation {
    insertInto("account")
      .columns("id", "name", "email", "gender", "role", "status")
      .scalaValues("1", "adminname", "admin@email.com", "0", "1", "1")
      .scalaValues("2", "username", "user@email.com", "1", "0", "1")
      .scalaValues("3", "newuser", "newuser@email.com", null, "0", "0")
      .build
  }

}

object UserFixture {
  val admin = User(1, None, None, None, User.Role.Admin, User.Status.Approved)
}
