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

package controllers.api

import org.davidbild.tristate.Tristate
import play.api.mvc.QueryStringBindable

object TristateQueryBinder {

  /**
    * Query binder for tristate.
    * Unspecified if query param not specified.
    * Absent if query param equals 'null'
    * Present if query param is present and can be bind by inner binder.
    */
  implicit def tristateQueryBinder[A](
    implicit stringBinder: QueryStringBindable[String],
    innerBinder: QueryStringBindable[A]
  ): QueryStringBindable[Tristate[A]] = new QueryStringBindable[Tristate[A]] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Tristate[A]]] = {
      stringBinder.bind(key, params) match {
        case None                              => Some(Right(Tristate.Unspecified))
        case Some(Left(error))                 => Some(Left(error))
        case Some(Right(str)) if str == "null" => Some(Right(Tristate.Absent))
        case Some(Right(_))                    => bindInner(key, params)
      }
    }

    /**
      * Binds inner tristate value.
      */
    private def bindInner(key: String, params: Map[String, Seq[String]]): Option[Either[String, Tristate[A]]] = {
      innerBinder.bind(key, params) match {
        case None               => Some(Right(Tristate.Unspecified))
        case Some(Left(error))  => Some(Left(error))
        case Some(Right(inner)) => Some(Right(Tristate.present(inner)))
      }
    }

    override def unbind(key: String, value: Tristate[A]): String = {
      val str = value match {
        case Tristate.Present(inner) => inner.toString
        case Tristate.Absent         => "null"
        case Tristate.Unspecified    => ""
      }
      stringBinder.unbind(key, str)
    }
  }
}
