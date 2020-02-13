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
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.mvc.QueryStringBindable

/**
  * Test for tristate query binder.
  */
class TristateQueryBinderTest extends PlaySpec with ScalaCheckDrivenPropertyChecks {

  private val stringBinder = QueryStringBindable.bindableString

  "tristate query binder" should {
    "bind unspecified values" in {
      val queryBinder = TristateQueryBinder.tristateQueryBinder(stringBinder, stringBinder)

      val result = queryBinder.bind("example", Map.empty)

      result mustBe Some(Right(Tristate.Unspecified))
    }

    "bind absent values" in {
      val queryBinder = TristateQueryBinder.tristateQueryBinder(stringBinder, stringBinder)

      val result = queryBinder.bind("example", Map("example" -> Seq("null")))

      result mustBe Some(Right(Tristate.Absent))
    }

    "bind present values" in {
      forAll { (value: String) =>
        whenever(value.nonEmpty && value != "null") {
          val queryBinder = TristateQueryBinder.tristateQueryBinder(stringBinder, stringBinder)

          val result = queryBinder.bind("example", Map("example" -> Seq(value)))

          result mustBe Some(Right(Tristate.present(value)))
        }
      }
    }
  }
}
