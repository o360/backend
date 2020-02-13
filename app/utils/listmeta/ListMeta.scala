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

package utils.listmeta

import utils.listmeta.pagination.Pagination
import utils.listmeta.sorting.Sorting

/**
  * List request meta information.
  *
  * @param pagination pagination
  * @param sorting    sorting
  */
case class ListMeta(pagination: Pagination, sorting: Sorting)

object ListMeta {

  /**
    * Default list meta. Disabled sorting and pagination.
    */
  def default: ListMeta = ListMeta(
    Pagination.WithoutPages,
    Sorting(Nil)
  )
}
