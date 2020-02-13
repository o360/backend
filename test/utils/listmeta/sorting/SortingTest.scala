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

import org.scalacheck.Gen
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/**
  * Test for sorting.
  */
class SortingTest extends PlaySpec with ScalaCheckDrivenPropertyChecks {

  "create" should {
    "create ascending sorting when sort is specified and fields are available" in {
      val availableFields = Set("one", "two", "three", "four")
      forAll(Gen.someOf(availableFields)) { fields =>
        val queryString =
          if (fields.nonEmpty) Map("sort" -> fields.mkString(",")) else Map[String, String]()
        val sorting = SortingRequestParser.parse(queryString)(Sorting.AvailableFields(availableFields))
        sorting mustBe Right(Sorting(fields.toSeq.map(Sorting.Field(_, Sorting.Direction.Asc))))
      }
    }

    "create descending sorting when sort is specified and fields are available" in {
      val availableFields = Set("one", "two", "three", "four")
      forAll(Gen.someOf(availableFields)) { fields =>
        val queryString =
          if (fields.nonEmpty) Map("sort" -> fields.map("-" + _).mkString(",")) else Map[String, String]()
        val sorting = SortingRequestParser.parse(queryString)(Sorting.AvailableFields(availableFields))
        sorting mustBe Right(Sorting(fields.toSeq.map(Sorting.Field(_, Sorting.Direction.Desc))))
      }
    }

    "return error if sort is empty" in {
      val sorting = SortingRequestParser.parse(Map("sort" -> ""))(Sorting.AvailableFields("one"))

      sorting mustBe a[Left[_, _]]
    }

    "return error if sorting is unparseable" in {
      forAll { (sort: String) =>
        whenever(!sort.matches("""-?\w+(,-?\w+)*""")) {
          val sorting = SortingRequestParser.parse(Map("sort" -> sort))(Sorting.AvailableFields("one"))

          sorting mustBe a[Left[_, _]]
        }
      }
    }

    "return error if sorting field not supported" in {
      val availableFields = Set("one", "two", "three", "four")
      forAll { fields: Seq[String] =>
        whenever(fields.exists(!availableFields.contains(_))) {
          val queryString = Map("sort" -> fields.mkString(","))
          val sorting = SortingRequestParser.parse(queryString)(Sorting.AvailableFields(availableFields))
          sorting mustBe a[Left[_, _]]
        }
      }
    }
  }
}
