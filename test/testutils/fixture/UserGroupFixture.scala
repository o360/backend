package testutils.fixture

import com.ninja_squad.dbsetup.Operations._

/**
  * User groups fixture.
  */
trait UserGroupFixture extends FixtureHelper with UserFixture with GroupFixture {
  self: FixtureSupport =>

  val UserGroups = Seq(
    (1, 1),
    (1, 2),
    (2, 3),
    (2, 5)
  )

  addFixtureOperation {
    val builder = insertInto("user_group")
      .columns("user_id", "group_id")

    UserGroups.foreach { x =>
      builder.scalaValues(x._1, x._2)
    }

    builder.build
  }
}
