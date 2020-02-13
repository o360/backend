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

package testutils.generator

import org.scalacheck.{Arbitrary, Gen}
import utils.listmeta.ListMeta
import utils.listmeta.pagination.Pagination
import utils.listmeta.sorting.Sorting

/**
  * ListMeta generator for scalacheck.
  */
trait ListMetaGenerator {

  implicit val listMetaArb = Arbitrary {
    val paginationGen: Gen[Pagination] = {
      val withPagesGen = for {
        size <- Arbitrary.arbitrary[Int]
        number <- Arbitrary.arbitrary[Int]
      } yield Pagination.WithPages(size, number)

      Gen.oneOf(Gen.const(Pagination.WithoutPages), withPagesGen)
    }

    val sortingGen = {
      val fieldGen = for {
        name <- Arbitrary.arbitrary[String]
        direction <- Gen.oneOf(Sorting.Direction.Asc, Sorting.Direction.Desc)
      } yield Sorting.Field(name, direction)

      Gen.listOf(fieldGen).map(Sorting(_))
    }
    for {
      pagination <- paginationGen
      sorting <- sortingGen
    } yield ListMeta(pagination, sorting)
  }
}
