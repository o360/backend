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

package utils.listmeta.sorting

/**
  * Sorting model.
  *
  * @param fields sort by fields.
  */
case class Sorting(fields: Seq[Sorting.Field])

object Sorting {

  /**
    * Sort by field.
    *
    * @param name      field name
    * @param direction sorting direction
    */
  case class Field(name: String, direction: Direction)

  /**
    * Sorting direction.
    */
  sealed trait Direction
  object Direction {

    /**
      * Ascending direction.
      */
    case object Asc extends Direction

    /**
      * Descending direction.
      */
    case object Desc extends Direction

  }

  /**
    * List of fields available for sorting.
    */
  case class AvailableFields(fields: Set[String])
  object AvailableFields {

    /**
      * Creates available sorting fields.
      *
      * @param fields fields list
      */
    def apply(fields: String*): AvailableFields = AvailableFields(fields.toSet)

    /**
      * Default sorting fields.
      */
    def default: AvailableFields = AvailableFields()
  }
}
