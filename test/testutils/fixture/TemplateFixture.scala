package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.template.Template
import models.notification._

/**
  * Templates fixture.
  */
trait TemplateFixture extends FixtureHelper { self: FixtureSupport =>

  val Templates = Seq(
    Template(1, "firstname", "firstsubject", "firstbody", PreBegin, Respondent),
    Template(2, "secondname", "secondsubject", "secondbody", End, Auditor),
    Template(3, "thirdname", "secondsubject", "thirdbody", Begin, Respondent)
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
