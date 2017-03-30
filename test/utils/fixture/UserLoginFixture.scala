package utils.fixture

import com.ninja_squad.dbsetup.Operations.insertInto

/**
  * User login fixture.
  */
trait UserLoginFixture extends UserFixture with FixtureHelper {
  self: FixtureSupport =>

  val UserLogins = Seq(
    (1, "prov-id-1", "prov-key-1"),
    (1, "prov-id-2", "prov-key-2"),
    (2, "prov-id-1", "prov-key-3")
  )

  addFixtureOperation {
    val builder = insertInto("user_login")
      .columns("user_id", "provider_id", "provider_key")

    UserLogins.foreach { case (userId, provId, provKey) =>
      builder.scalaValues(userId, provId, provKey)
    }

    builder.build
  }
}
