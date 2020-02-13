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
  * Entity with ID and name.
  */
case class NamedEntity(
  id: Long,
  name: Option[String]
) {
  override def equals(obj: scala.Any): Boolean = {
    if (super.equals(obj)) true
    else if (obj == null) false
    else
      obj match {
        case that: NamedEntity => this.id == that.id
        case _                 => false
      }
  }

  override def hashCode(): Int = id.##
}

object NamedEntity {

  /**
    * Creates entity using ID.
    */
  def apply(id: Long): NamedEntity = new NamedEntity(id, None)

  /**
    * Creates entity using ID and name.
    */
  def apply(id: Long, name: String): NamedEntity = new NamedEntity(id, Some(name))
}
