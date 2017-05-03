package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.project.Project

/**
  * Project model fixture.
  */
trait ProjectFixture extends FixtureHelper with GroupFixture {
  self: FixtureSupport =>

  val Projects = Seq(
    Project(
      1,
      "first",
      Some("description"),
      3
    ),
    Project(
      2,
      "second",
      None,
      1
    )
  )

  addFixtureOperation {
    insertInto("project")
      .columns("id", "name", "description", "group_auditor_id")
      .scalaValues(1, "first", "description", 3)
      .scalaValues(2, "second", null, 1)
      .build
  }
}
