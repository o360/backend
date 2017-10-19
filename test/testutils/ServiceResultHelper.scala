package testutils

import utils.errors.ApplicationError

import scala.concurrent.Future
import scalaz.{\/, EitherT}

/**
  * Service result creation helper.
  */
trait ServiceResultHelper extends AsyncHelper {

  private type E = ApplicationError

  def toErrorResult[A](error: E): EitherT[Future, E, A] =
    EitherT.eitherT(toFuture(\/.left[E, A](error)))

  def toSuccessResult[A](result: A): EitherT[Future, E, A] =
    EitherT.eitherT(toFuture(\/.right[E, A](result)))
}
