package testutils.fixture

import com.ninja_squad.dbsetup.Operations._

/**
  * Event project fixture.
  */
trait EventProjectFixture extends FixtureHelper with EventFixture with ProjectFixture {
  self: FixtureSupport =>

  val EventProjects = Seq(
    (1, 1),
    (1, 2),
    (2, 2),
    (3, 1)
  )

  addFixtureOperation {
    val builder = insertInto("event_project")
      .columns("event_id", "project_id")

    EventProjects.foreach { x =>
      builder.scalaValues(x._1, x._2)
    }

    builder.build
  }
}
