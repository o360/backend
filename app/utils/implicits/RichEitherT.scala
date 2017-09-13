package utils.implicits

import scalaz._
import Scalaz._
import scala.concurrent.{ExecutionContext, Future}

/**
  * EitherT extensions.
  */
object RichEitherT {

  implicit class SeqEitherTDecorator[E, A](s: Seq[EitherT[Future, E, A]]) {

    def sequenced(implicit ec: ExecutionContext): EitherT[Future, E, Seq[A]] = collected.leftMap(_.head)

    def collected(implicit ec: ExecutionContext): EitherT[Future, Seq[E], Seq[A]] = {
      val future = Future.sequence(s.map(_.run)).map { results =>
        val lefts = results.collect { case -\/(e) => e }

        if (lefts.nonEmpty) -\/(lefts) else \/-(results.collect { case \/-(a) => a })
      }

      EitherT.eitherT(future)
    }
  }
}
