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

package services

import models.event.Event
import models.user.User
import org.scalacheck.{Arbitrary, Gen}
import testutils.generator.{EventGenerator, UserGenerator}

/**
  * Test for template engine service.
  */
class TemplateEngineServiceTest extends BaseServiceTest with UserGenerator with EventGenerator {

  val service = new TemplateEngineService()

  "render" should {
    "replace key with context value" in {
      forAll(Gen.alphaStr, Arbitrary.arbitrary[String]) { (key: String, value: String) =>
        whenever(key.matches("[a-zA-Z]+")) {
          val context = Map(key -> value)
          val template = s"{{$key}}"
          val result = service.render(template, context)
          result mustBe value
        }
      }
    }

    "replace key with empty string if not in context" in {
      forAll(Gen.alphaStr, Gen.alphaStr) { (key: String, contextKey: String) =>
        whenever(key.nonEmpty && contextKey.nonEmpty && key != contextKey) {
          val context = Map(contextKey -> "anystring")
          val template = s"{{$key}}"
          val result = service.render(template, context)
          result mustBe ""
        }
      }
    }

    "replace only keys" in {
      forAll { template: String =>
        whenever(!template.contains("{") && !template.contains("}")) {}
        val result = service.render(template, Map())

        result mustBe template
      }
    }
  }

  "getContext" should {
    "return valid context" in {
      forAll { (user: User, event: Event) =>
        val result = service.getContext(user, Some(event))

        result must not be empty
        result("user_name") mustBe user.fullName.getOrElse("")
        result("event_description") mustBe event.description.getOrElse("")
        result.get("event_start") mustBe defined
        result.get("event_end") mustBe defined
      }
    }
  }
}
