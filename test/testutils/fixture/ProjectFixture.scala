package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.NamedEntity
import models.notification.Notification
import models.project.{Project, TemplateBinding}

/**
  * Project model fixture.
  */
trait ProjectFixture extends FixtureHelper with GroupFixture with TemplateFixture {
  self: FixtureSupport =>

  val Projects = Seq(
    Project(
      1,
      "first",
      Some("description"),
      NamedEntity(3, Groups.find(_.id == 3).get.name),
      Seq(
        TemplateBinding(NamedEntity(1, "firstname"), Notification.Kind.Begin, Notification.Recipient.Respondent),
        TemplateBinding(NamedEntity(2, "secondname"), Notification.Kind.End, Notification.Recipient.Respondent),
        TemplateBinding(NamedEntity(2, "secondname"), Notification.Kind.End, Notification.Recipient.Auditor)
      )
    ),
    Project(
      2,
      "second",
      None,
      NamedEntity(1, Groups.find(_.id == 1).get.name),
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
      insertInto("project_email_template")
        .columns("project_id", "template_id", "kind", "recipient_kind")
        .scalaValues(1, 1, 1, 0)
        .scalaValues(1, 2, 3, 0)
        .scalaValues(1, 2, 3, 1)
        .build
    )

  }
}
