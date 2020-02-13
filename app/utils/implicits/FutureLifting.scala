/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils.implicits

import utils.errors.ApplicationError

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._
import scalaz.{\/, EitherT, Monad, OptionT}

/**
  * Implicits for working with futures.
  */
object FutureLifting {

  implicit def FutureMonad(implicit ec: ExecutionContext) = new Monad[Future] {
    def point[A](a: => A): Future[A] = Future(a)
    def bind[A, B](fa: Future[A])(f: (A) => Future[B]): Future[B] = fa flatMap f
  }

  implicit class AnyDecorator[A](a: A) {

    /**
      * Converts value to future.
      */
    def toFuture: Future[A] = Future.successful(a)

    /**
      * Lifts value to the right.
      */
    def lift: EitherT[Future, ApplicationError, A] = {
      EitherT.eitherT(a.right[ApplicationError].toFuture)
    }
  }

  implicit class FutureOptionDecorator[A](fo: Future[Option[A]]) {

    /**
      * Lifts option value to the right. If option is empty, lifts error to the left.
      *
      * @param error error for empty option
      */
    def liftRight(error: => ApplicationError)(implicit ec: ExecutionContext): EitherT[Future, ApplicationError, A] = {
      OptionT.optionT(fo).toRight(error)
    }
  }

  implicit class OptionErrorDecorator[E <: ApplicationError](o: Option[E]) {

    /**
      * Lifts option value to the left. If option is empty, lifts Unit to the right.
      */
    def liftLeft: EitherT[Future, ApplicationError, Unit] = {
      EitherT.eitherT(Future.successful(\/.fromEither(o.toLeft(()))))
    }
  }

  implicit class FutureDecorator[A](f: Future[A]) {

    /**
      * Lifts futures value to the right.
      */
    def lift(implicit ec: ExecutionContext): EitherT[Future, ApplicationError, A] = {
      EitherT.eitherT(f.map(_.right[ApplicationError]))
    }

    /**
      * Lifts future value to the right.
      * If future is failed, tries to recover it with pf
      *
      * @param pf exceptions mapping
      */
    def lift(
      pf: PartialFunction[Throwable, ApplicationError]
    )(implicit ec: ExecutionContext): EitherT[Future, ApplicationError, A] = {
      EitherT.eitherT(f.map(_.right[ApplicationError]).recover(pf.andThen(_.left)))
    }
  }

  implicit class FutureBooleanDecorator(val fb: Future[Boolean]) {

    def &&(other: => Future[Boolean])(implicit ec: ExecutionContext): Future[Boolean] =
      fb.flatMap(!_ ? false.toFuture | other)

    def ||(other: => Future[Boolean])(implicit ec: ExecutionContext): Future[Boolean] =
      fb.flatMap(_ ? true.toFuture | other)

    def unary_!(implicit ec: ExecutionContext): Future[Boolean] = fb.map(!_)
  }

  implicit class EitherDecorator[E <: ApplicationError, A](val either: E \/ A) {

    /**
      * Converts either to EitherT.
      */
    def lift: EitherT[Future, ApplicationError, A] = {
      EitherT.eitherT(either.leftMap(_.asInstanceOf[ApplicationError]).toFuture)
    }
  }

  /**
    * If condition is false, lifts error to the left, otherwise lifts Unit to the right.
    *
    * @param conditionF future of condition
    * @param error      error for negative condition
    */
  def ensure(
    conditionF: Future[Boolean]
  )(error: => ApplicationError)(implicit ec: ExecutionContext): EitherT[Future, ApplicationError, Unit] = {
    EitherT.eitherT(conditionF.map(_ ? ().right[ApplicationError] | error.left[Unit]))
  }

  /**
    * If condition is false, lifts error to the left, otherwise lifts Unit to the right.
    *
    * @param condition condition
    * @param error     error for negative condition
    */
  def ensure(condition: Boolean)(error: => ApplicationError): EitherT[Future, ApplicationError, Unit] = {
    EitherT.eitherT((condition ? ().right[ApplicationError] | error.left[Unit]).toFuture)
  }
}
