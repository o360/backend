package services

import akka.actor.ActorSystem
import org.scalatest.{OneInstancePerTest, ParallelTestExecution}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import testutils.fixture.FixtureSupport
import testutils.{AsyncHelper, MockitoHelper, ScalazDisjunctionMatchers, ServiceResultHelper}

/**
  * Base trait for service tests.
  */
trait BaseServiceTest
  extends PlaySpec
  with ScalaCheckDrivenPropertyChecks
  with AsyncHelper
  with ScalazDisjunctionMatchers
  with MockitoSugar
  with FixtureSupport
  with OneInstancePerTest
  with ServiceResultHelper
  with MockitoHelper
  with ParallelTestExecution {

  val ec = scala.concurrent.ExecutionContext.global
  val actorSystem = ActorSystem("default")

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(sizeRange = 15, workers = 4)
}
