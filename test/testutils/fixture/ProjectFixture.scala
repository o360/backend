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
      3,
      Seq(
        Project.Relation(1, Some(2), 1, Project.RelationKind.Classic),
        Project.Relation(2, None, 2, Project.RelationKind.Survey)
      )
    ),
    Project(
      2,
      "second",
      None,
      1,
      Nil
    )
  )

  addFixtureOperation {
    sequenceOf(
      insertInto("project")
        .columns("id", "name", "description", "group_auditor_id")
        .scalaValues(1, "first", "description", 3)
        .scalaValues(2, "second", null, 1)
        .build,
      insertInto("relation")
        .columns("project_id", "group_from_id", "group_to_id", "form_id", "kind")
        .scalaValues(1, 1, 2, 1, 0)
        .scalaValues(1, 2, null, 2, 1)
        .build
    )
  }
}
