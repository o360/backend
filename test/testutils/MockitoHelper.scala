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

import scala.reflect.ClassTag
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any => mockitoAny, eq => mockitoEq}

/**
  * Helper for mockito mocking framework.
  */
trait MockitoHelper {

  /**
    * Returns argumentCaptor for given class.
    */
  def argumentCaptor[A](implicit classTag: ClassTag[A]): ArgumentCaptor[A] = {
    ArgumentCaptor.forClass(classTag.runtimeClass.asInstanceOf[Class[A]])
  }

  /**
    * Scala version for mockitoAny matcher.
    */
  def *[A]: A = mockitoAny[A]

  /**
    * Scala version for mockito eq matcher.
    */
  def eqTo[A](a: A): A = mockitoEq(a)
}
