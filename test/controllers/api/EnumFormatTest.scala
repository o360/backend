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

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.{JsError, JsString, JsSuccess}
import testutils.ScalazDisjunctionMatchers

/**
  * Test for enum format.
  */
class EnumFormatTest extends PlaySpec with ScalaCheckDrivenPropertyChecks with ScalazDisjunctionMatchers {

  private trait TestEnum
  private case object TestValue1 extends TestEnum
  private case object TestValue2 extends TestEnum

  private case class TestEnumFormat(value: TestEnum) extends EnumFormat[TestEnum]
  private object TestEnumFormat extends EnumFormatHelper[TestEnum, TestEnumFormat]("test") {

    /**
      * Mapping between string representation and concrete enum values.
      */
    override protected def mapping: Map[String, TestEnum] = Map(
      "value1" -> TestValue1,
      "value2" -> TestValue2
    )
  }

  "enum format" should {
    "generate valid json writes" in {
      val writes = TestEnumFormat.writes
      val result = writes.writes(TestEnumFormat(TestValue1))
      val result2 = writes.writes(TestEnumFormat(TestValue2))

      result mustBe a[JsString]
      result.as[String] mustBe "value1"

      result2 mustBe a[JsString]
      result2.as[String] mustBe "value2"
    }

    "generate valid json reads" in {
      val reads = TestEnumFormat.reads
      val result = reads.reads(JsString("value1"))
      val result2 = reads.reads(JsString("value2"))
      val result3 = reads.reads(JsString("value3"))

      result mustBe JsSuccess(TestEnumFormat(TestValue1))
      result2 mustBe JsSuccess(TestEnumFormat(TestValue2))
      result3 mustBe a[JsError]
    }

    "generate valid query string binder" in {
      val binder = TestEnumFormat.queryBinder

      val queryKey = "example"
      val result = binder.bind(queryKey, Map(queryKey -> Seq("value1")))
      val result2 = binder.bind(queryKey, Map(queryKey -> Seq("value2")))
      val result3 = binder.bind(queryKey, Map(queryKey -> Seq("value3")))
      val result4 = binder.bind(queryKey, Map.empty)

      result mustBe Some(Right(TestEnumFormat(TestValue1)))
      result2 mustBe Some(Right(TestEnumFormat(TestValue2)))
      result3.get mustBe a[Left[_, _]]
      result4 mustBe empty
    }
  }
}
