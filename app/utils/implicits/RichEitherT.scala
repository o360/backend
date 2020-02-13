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
