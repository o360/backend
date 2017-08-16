package utils.listmeta.pagination

import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec

/**
  * Test for pagination.
  */
class PaginationTest extends PlaySpec with GeneratorDrivenPropertyChecks {

  "create" should {
    "create pagination when both size and number specified" in {
      forAll(Gen.choose(0, 1000), Gen.choose(1, 1000)) { (size: Int, number: Int) =>
        val pagination = PaginationRequestParser.parse(Map("size" -> size.toString, "number" -> number.toString))

        pagination mustBe Right(Pagination.WithPages(size, number))
      }
    }

    "create pagination when only size specified" in {
      forAll { (size: Int) =>
        whenever(size >= 0) {
          val pagination = PaginationRequestParser.parse(Map("size" -> size.toString))

          pagination mustBe Right(Pagination.WithPages(size, 1))
        }
      }
    }

    "create pagination without pages when number not specified" in {
      val pagination = PaginationRequestParser.parse(Map())

      pagination mustBe Right(Pagination.WithoutPages)
    }

    "return error if size is less than zero" in {
      forAll { (size: Int) =>
        whenever(size < 0) {
          val pagination = PaginationRequestParser.parse(Map("size" -> size.toString))

          pagination mustBe 'isLeft
        }
      }
    }

    "return error if number is less or equal zero" in {
      forAll { (number: Int) =>
        whenever(number <= 0) {
          val pagination = PaginationRequestParser.parse(Map("number" -> number.toString, "size" -> "1"))

          pagination mustBe 'isLeft
        }
      }
    }

    "return error if size is unparseable" in {
      forAll { (size: String) =>
        whenever(!size.matches("\\d+")) {
          val pagination = PaginationRequestParser.parse(Map("size" -> size))

          pagination mustBe 'isLeft
        }
      }
    }

    "return error if number is unparseable" in {
      forAll { (number: String) =>
        whenever(!number.matches("\\d+")) {
          val pagination = PaginationRequestParser.parse(Map("number" -> number, "size" -> "1"))

          pagination mustBe 'isLeft
        }
      }
    }
  }
}
