package testutils.generator

import models.project.ActiveProject
import org.scalacheck.{Arbitrary, Gen}
import testutils.fixture.{EventFixture, ProjectFixture}

/**
  * Active project generator for scalacheck.
  */
trait ActiveProjectGenerator {

  implicit val activeProjectArb = Arbitrary {
    for {
      id <- Arbitrary.arbitrary[Long]
      eventId <- Gen.oneOf(EventFixture.values.map(_.id))
      name <- Arbitrary.arbitrary[String]
      description <- Arbitrary.arbitrary[Option[String]]
      formsOnSamePage <- Arbitrary.arbitrary[Boolean]
      canRevote <- Arbitrary.arbitrary[Boolean]
      isAnonymous <- Arbitrary.arbitrary[Boolean]
      machineName <- Arbitrary.arbitrary[String]
      parentProjectId <- Gen.option(Gen.oneOf(ProjectFixture.values.map(_.id)))
    } yield
      ActiveProject(
        id,
        eventId,
        name,
        description,
        formsOnSamePage,
        canRevote,
        isAnonymous,
        machineName,
        parentProjectId
      )
  }
}
