package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.project.ActiveProject

/**
  * Active project model fixture.
  */
trait ActiveProjectFixture extends FixtureHelper with EventFixture with UserFixture with ProjectFixture {
  self: FixtureSupport =>

  val ActiveProjects = Seq(
    ActiveProject(
      1,
      1,
      "first",
      Some("description"),
      formsOnSamePage = true,
      canRevote = true,
      isAnonymous = true,
      machineName = "some machine name",
      parentProjectId = Some(1)
    ),
    ActiveProject(
      2,
      2,
      "second",
      None,
      formsOnSamePage = false,
      canRevote = false,
      isAnonymous = false,
      machineName = "another machine name",
      parentProjectId = None
    )
  )

  addFixtureOperation {
    sequenceOf(
      insertInto("active_project")
        .columns("id",
                 "event_id",
                 "name",
                 "description",
                 "forms_on_same_page",
                 "can_revote",
                 "is_anonymous",
                 "machine_name",
                 "parent_project_id")
        .scalaValues(1, 1, "first", "description", true, true, true, "some machine name", 1)
        .scalaValues(2, 2, "second", null, false, false, false, "another machine name", null)
        .build,
      insertInto("active_project_auditor")
        .columns("user_id", "active_project_id")
        .scalaValues(1, 1)
        .scalaValues(2, 1)
        .scalaValues(1, 2)
        .build
    )

  }
}
