package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.project.ActiveProject

/**
  * Active project model fixture.
  */
trait ActiveProjectFixture extends FixtureHelper with EventFixture with UserFixture with ProjectFixture {
  self: FixtureSupport =>

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

object ActiveProjectFixture {
  val values = Seq(
    ActiveProject(
      id = 1,
      eventId = 1,
      name = "first",
      description = Some("description"),
      formsOnSamePage = true,
      canRevote = true,
      isAnonymous = true,
      machineName = "some machine name",
      parentProjectId = Some(1)
    ),
    ActiveProject(
      id = 2,
      eventId = 2,
      name = "second",
      description = None,
      formsOnSamePage = false,
      canRevote = false,
      isAnonymous = false,
      machineName = "another machine name",
      parentProjectId = None
    )
  )

  case class ProjectAuditor(userId: Long, projectId: Long)

  val auditorValues = Seq(
    ProjectAuditor(1, 1),
    ProjectAuditor(2, 1),
    ProjectAuditor(1, 2)
  )
}
