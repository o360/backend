package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.NamedEntity
import models.notification._
import models.project.{Project, TemplateBinding}

/**
  * Project model fixture.
  */
trait ProjectFixture extends FixtureHelper with GroupFixture with TemplateFixture { self: FixtureSupport =>

  val Projects = ProjectFixture.values

  addFixtureOperation {
    sequenceOf(
      insertInto("project")
        .columns(
          "id",
          "name",
          "description",
          "group_auditor_id",
          "forms_on_same_page",
          "can_revote",
          "is_anonymous",
          "machine_name"
        )
        .scalaValues(1, "first", "description", 3, true, true, true, "some machine name")
        .scalaValues(2, "second", null, 1, false, false, false, "another machine name")
        .build,
      insertInto("project_email_template")
        .columns("project_id", "template_id", "kind", "recipient_kind")
        .scalaValues(1, 1, 1, 0)
        .scalaValues(1, 2, 3, 0)
        .scalaValues(1, 2, 3, 1)
        .build
    )

  }
}

object ProjectFixture {
  val values = Seq(
    Project(
      1,
      "first",
      Some("description"),
      NamedEntity(3, GroupFixture.values.find(_.id == 3).get.name),
      Seq(
        TemplateBinding(NamedEntity(1, "firstname"), Begin, Respondent),
        TemplateBinding(NamedEntity(2, "secondname"), End, Respondent),
        TemplateBinding(NamedEntity(2, "secondname"), End, Auditor)
      ),
      formsOnSamePage = true,
      canRevote = true,
      isAnonymous = true,
      hasInProgressEvents = false,
      machineName = "some machine name"
    ),
    Project(
      2,
      "second",
      None,
      NamedEntity(1, GroupFixture.values.find(_.id == 1).get.name),
      Nil,
      formsOnSamePage = false,
      canRevote = false,
      isAnonymous = false,
      hasInProgressEvents = false,
      machineName = "another machine name"
    )
  )
}
