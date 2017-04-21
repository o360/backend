package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.project.Project

/**
  * Project model fixture.
  */
trait ProjectFixture extends FixtureHelper with GroupFixture with FormFixture {
  self: FixtureSupport =>

  val Projects = Seq(
    Project(
      1,
      "first",
      Some("description"),
      Seq(
        Project.Relation(1, 2, 3, 1),
        Project.Relation(2, 3, 1, 2)
      )
    ),
    Project(
      2,
      "second",
      None,
      Nil
    )
  )

  addFixtureOperation {
    sequenceOf(
      insertInto("project")
        .columns("id", "name", "description")
        .scalaValues(1, "first", "description")
        .scalaValues(2, "second", null)
        .build,
      insertInto("relation")
        .columns("project_id", "group_from_id", "group_to_id", "group_auditor_id", "form_id")
        .scalaValues(1, 1, 2, 3, 1)
        .scalaValues(1, 2, 3, 1, 2)
        .build
    )
  }
}
