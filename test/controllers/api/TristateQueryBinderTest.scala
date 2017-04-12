package controllers.api

import org.davidbild.tristate.Tristate
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import play.api.mvc.QueryStringBindable

/**
  * Test for tristate query binder.
  */
class TristateQueryBinderTest extends PlaySpec with GeneratorDrivenPropertyChecks {

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
