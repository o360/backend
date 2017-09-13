package services

import org.scalatest.OneInstancePerTest
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import testutils.AsyncHelper
import testutils.fixture.FixtureSupport
import utils.errors.ApplicationError

import scala.concurrent.Future
import scalaz.{\/, EitherT}

/**
  * Base trait for service tests.
  */
trait BaseServiceTest
  extends PlaySpec
  with GeneratorDrivenPropertyChecks
  with AsyncHelper
  with MockitoSugar
  with FixtureSupport
  with OneInstancePerTest {

  val ec = scala.concurrent.ExecutionContext.global

  def toErrorResult[A](error: ApplicationError): EitherT[Future, ApplicationError, A] =
    EitherT.eitherT(toFuture(\/.left[ApplicationError, A](error)))

  def toSuccessResult[A](result: A): EitherT[Future, ApplicationError, A] =
    EitherT.eitherT(toFuture(\/.right[ApplicationError, A](result)))
}
