package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.project.Relation

/**
  * Project relation fixture.
  */
trait ProjectRelationFixture extends FixtureHelper with ProjectFixture with GroupFixture with FormFixture {
  self: FixtureSupport =>

  val ProjectRelations = Seq(
    Relation(1, 1, 1, Some(2), 1, Relation.Kind.Classic),
    Relation(2, 1, 2, None, 2, Relation.Kind.Survey)
  )

  addFixtureOperation {
    insertInto("relation")
      .columns("id", "project_id", "group_from_id", "group_to_id", "form_id", "kind")
      .scalaValues(1, 1, 1, 2, 1, 0)
      .scalaValues(2, 1, 2, null, 2, 1)
      .build
  }
}
