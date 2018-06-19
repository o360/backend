package services

import akka.actor.ActorSystem
import org.scalatest.{OneInstancePerTest, ParallelTestExecution}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import testutils.fixture.FixtureSupport
import testutils.{AsyncHelper, MockitoHelper, ServiceResultHelper}

/**
  * Base trait for service tests.
  */
trait BaseServiceTest
  extends PlaySpec
  with GeneratorDrivenPropertyChecks
  with AsyncHelper
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
