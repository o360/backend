package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.group.{Group => GroupModel}


/**
  * Groups fixture.
  */
trait GroupFixture extends FixtureHelper {
  self: FixtureSupport =>

  val Groups = Seq(
    GroupModel(1, None, "1", hasChildren = false, level = 0),
    GroupModel(2, None, "2", hasChildren = true, level = 0),
    GroupModel(3, Some(2), "2-1", hasChildren = false, level = 0), // actual level is bigger, h2 limitations
    GroupModel(4, Some(2), "2-2", hasChildren = true, level = 0),
    GroupModel(5, Some(4), "2-2-1", hasChildren = false, level = 0)
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
