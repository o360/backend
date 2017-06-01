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
        result("user_name") mustBe user.name.getOrElse("")
        result("event_description") mustBe event.description.getOrElse("")
        result.get("event_start") mustBe defined
        result.get("event_end") mustBe defined
      }
    }
  }
}
