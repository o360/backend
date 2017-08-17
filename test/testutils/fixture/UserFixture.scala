package testutils.fixture

import java.time.ZoneOffset

import com.ninja_squad.dbsetup.Operations._
import models.user.User

/**
  * User's model fixture.
  */
trait UserFixture extends FixtureHelper { self: FixtureSupport =>
  val Users = Seq(
    User(1,
         Some("adminname"),
         Some("admin@email.com"),
         Some(User.Gender.Male),
         User.Role.Admin,
         User.Status.Approved,
         ZoneOffset.UTC,
         termsApproved = true),
    User(2,
         Some("username"),
         Some("user@email.com"),
         Some(User.Gender.Female),
         User.Role.User,
         User.Status.Approved,
         ZoneOffset.of("+07"),
         termsApproved = false),
    User(3,
         Some("newuser"),
         Some("newuser@email.com"),
         None,
         User.Role.User,
         User.Status.New,
         ZoneOffset.UTC,
         termsApproved = false)
  )

  addFixtureOperation {
    insertInto("account")
      .columns("id", "name", "email", "gender", "role", "status", "timezone", "terms_approved")
      .scalaValues("1", "adminname", "admin@email.com", "0", "1", "1", "Z", true)
      .scalaValues("2", "username", "user@email.com", "1", "0", "1", "+07", false)
      .scalaValues("3", "newuser", "newuser@email.com", null, "0", "0", "Z", false)
      .build
  }

}

object UserFixture {
  val admin = User(1, None, None, None, User.Role.Admin, User.Status.Approved, ZoneOffset.UTC, termsApproved = true)

  val user = User(1, None, None, None, User.Role.User, User.Status.Approved, ZoneOffset.UTC, termsApproved = true)
}
