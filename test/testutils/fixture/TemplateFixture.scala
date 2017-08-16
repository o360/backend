package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.template.Template
import models.notification.Notification

/**
  * Templates fixture.
  */
trait TemplateFixture extends FixtureHelper { self: FixtureSupport =>

  val Templates = Seq(
    Template(1,
             "firstname",
             "firstsubject",
             "firstbody",
             Notification.Kind.PreBegin,
             Notification.Recipient.Respondent),
    Template(2, "secondname", "secondsubject", "secondbody", Notification.Kind.End, Notification.Recipient.Auditor),
    Template(3, "thirdname", "secondsubject", "thirdbody", Notification.Kind.Begin, Notification.Recipient.Respondent)
  )

  addFixtureOperation {
    insertInto("template")
      .columns("id", "name", "subject", "body", "kind", "recipient_kind")
      .scalaValues(1, "firstname", "firstsubject", "firstbody", 0, 0)
      .scalaValues(2, "secondname", "secondsubject", "secondbody", 3, 1)
      .scalaValues(3, "thirdname", "secondsubject", "thirdbody", 1, 0)
      .build
  }
}
