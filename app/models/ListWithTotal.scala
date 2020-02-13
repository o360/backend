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

package models

/**
  * List result with total count info.
  *
  * @param total total count
  * @param data  list elements
  */
case class ListWithTotal[A](total: Int, data: Seq[A])

object ListWithTotal {
  def apply[A](data: Seq[A]): ListWithTotal[A] = ListWithTotal(data.size, data)
}
