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

package testutils

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Helper for scala's future.
  */
trait AsyncHelper {

  /**
    * Waits for future competition and returns it's value.
    */
  def wait[T](future: Future[T]): T = Await.result(future, Duration(5, "second"))

  /**
    * Transforms given value to successful future.
    */
  def toFuture[T](value: T): Future[T] = Future.successful(value)

}
