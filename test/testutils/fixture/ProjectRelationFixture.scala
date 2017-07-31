package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.NamedEntity
import models.notification.Notification
import models.project.{Relation, TemplateBinding}

/**
  * Project relation fixture.
  */
trait ProjectRelationFixture
  extends FixtureHelper
  with ProjectFixture
  with GroupFixture
  with FormFixture
  with TemplateFixture {
  self: FixtureSupport =>

  val ProjectRelations = Seq(
    Relation(
      id = 1,
      project = NamedEntity(1, Projects.find(_.id == 1).get.name),
      groupFrom = NamedEntity(1, Groups.find(_.id == 1).get.name),
      groupTo = Some(NamedEntity(2, Groups.find(_.id == 2).get.name)),
      form = NamedEntity(1, Forms.find(_.id == 1).get.name),
      kind = Relation.Kind.Classic,
      templates = Seq(
        TemplateBinding(NamedEntity(1, "firstname"), Notification.Kind.Begin, Notification.Recipient.Respondent),
        TemplateBinding(NamedEntity(2, "secondname"), Notification.Kind.End, Notification.Recipient.Respondent),
        TemplateBinding(NamedEntity(2, "secondname"), Notification.Kind.End, Notification.Recipient.Auditor)
      ),
      hasInProgressEvents = false,
      canSelfVote = true
    ),
    Relation(
      id = 2,
      project = NamedEntity(1, Projects.find(_.id == 1).get.name),
      groupFrom = NamedEntity(2, Groups.find(_.id == 2).get.name),
      groupTo = None,
      form = NamedEntity(2, Forms.find(_.id == 2).get.name),
      kind = Relation.Kind.Survey,
      templates = Nil,
      hasInProgressEvents = false,
      canSelfVote = false
    )
  )

  addFixtureOperation {
    sequenceOf(
      insertInto("relation")
        .columns("id", "project_id", "group_from_id", "group_to_id", "form_id", "kind", "can_self_vote")
        .scalaValues(1, 1, 1, 2, 1, 0, true)
        .scalaValues(2, 1, 2, null, 2, 1, false)
        .build,
      insertInto("relation_email_template")
        .columns("relation_id", "template_id", "kind", "recipient_kind")
        .scalaValues(1, 1, 1, 0)
        .scalaValues(1, 2, 3, 0)
        .scalaValues(1, 2, 3, 1)
        .build
    )
  }
}
