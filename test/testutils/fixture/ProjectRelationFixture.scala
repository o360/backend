package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.NamedEntity
import models.project.Relation

/**
  * Project relation fixture.
  */
trait ProjectRelationFixture extends FixtureHelper with ProjectFixture with GroupFixture with FormFixture {
  self: FixtureSupport =>

  val ProjectRelations = Seq(
    Relation(
      id = 1,
      project = NamedEntity(1, Projects.find(_.id == 1).get.name),
      groupFrom = NamedEntity(1, Groups.find(_.id == 1).get.name),
      groupTo = Some(NamedEntity(2, Groups.find(_.id == 2).get.name)),
      form = NamedEntity(1, Forms.find(_.id == 1).get.name),
      kind = Relation.Kind.Classic
    ),
    Relation(
      id = 2,
      project = NamedEntity(1, Projects.find(_.id == 1).get.name),
      groupFrom = NamedEntity(2, Groups.find(_.id == 2).get.name),
      groupTo = None,
      form = NamedEntity(2, Forms.find(_.id == 2).get.name),
      kind = Relation.Kind.Survey
    )
  )

  addFixtureOperation {
    insertInto("relation")
      .columns("id", "project_id", "group_from_id", "group_to_id", "form_id", "kind")
      .scalaValues(1, 1, 1, 2, 1, 0)
      .scalaValues(2, 1, 2, null, 2, 1)
      .build
  }
}
