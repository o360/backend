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
