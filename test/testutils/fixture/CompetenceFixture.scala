package testutils.fixture

import com.ninja_squad.dbsetup.Operations.insertInto
import models.EntityKind
import models.competence.Competence

/**
  * Competence fixture.
  */
trait CompetenceFixture extends CompetenceGroupFixture with FixtureHelper { self: FixtureSupport =>

  addFixtureOperation {
    insertInto("competence")
      .columns("id", "group_id", "name", "description", "kind", "machine_name")
      .scalaValues(1, 1, "comp 1", "comp 1 desc", 0, "comp 1 machine name")
      .scalaValues(2, 1, "comp 2", null, 0, "comp 2 machine name")
      .scalaValues(3, 3, "comp 1", "comp 1 desc", 1, "comp 1 machine name")
      .build
  }

  val Competences = CompetenceFixture.values
}

object CompetenceFixture {
  val values = Seq(
    Competence(
      id = 1,
      groupId = 1,
      name = "comp 1",
      description = Some("comp 1 desc"),
      kind = EntityKind.Template,
      machineName = "comp 1 machine name"
    ),
    Competence(
      id = 2,
      groupId = 1,
      name = "comp 2",
      description = None,
      kind = EntityKind.Template,
      machineName = "comp 2 machine name"
    ),
    Competence(
      id = 3,
      groupId = 3,
      name = "comp 1",
      description = Some("comp 1 desc"),
      kind = EntityKind.Freezed,
      machineName = "comp 1 machine name"
    )
  )
}
