package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.group.{Group => GroupModel}


/**
  * Groups fixture.
  */
trait GroupFixture extends FixtureHelper {
  self: FixtureSupport =>

  val Groups = Seq(
    GroupModel(1, None, "1"),
    GroupModel(2, None, "2"),
    GroupModel(3, Some(2), "2-1"),
    GroupModel(4, Some(2), "2-2"),
    GroupModel(5, Some(4), "2-2-1")
  )

  addFixtureOperation {
    val builder = insertInto("orgstructure")
      .columns("id", "parent_id", "name")

    Groups.foreach { g =>
      builder.scalaValues(g.id, g.parentId.orNull, g.name)
    }

    builder.build
  }
}
