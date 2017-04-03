package utils.listmeta.sorting

import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import testutils.generator.SymbolGenerator

/**
  * Test for sorting.
  */
class SortingTest extends PlaySpec with GeneratorDrivenPropertyChecks with SymbolGenerator {

  "create" should {
    "create ascending sorting when sort is specified and fields are available" in {
      val availableFields = Set('one, 'two, 'three, 'four)
      forAll(Gen.someOf(availableFields)) { (fields: Seq[Symbol]) =>
        val queryString = if (fields.nonEmpty) Map("sort" -> fields.map(_.name).mkString(",")) else Map[String, String]()
        val sorting = Sorting.create(queryString)(AvailableSortingFields(availableFields))
        sorting mustBe Right(Sorting(fields.map(SortField(_, Direction.Asc))))
      }
    }

    "create descending sorting when sort is specified and fields are available" in {
      val availableFields = Set('one, 'two, 'three, 'four)
      forAll(Gen.someOf(availableFields)) { (fields: Seq[Symbol]) =>
        val queryString = if (fields.nonEmpty) Map("sort" -> fields.map("-" + _.name).mkString(",")) else Map[String, String]()
        val sorting = Sorting.create(queryString)(AvailableSortingFields(availableFields))
        sorting mustBe Right(Sorting(fields.map(SortField(_, Direction.Desc))))
      }
    }

    "return error if sort is empty" in {
      val sorting = Sorting.create(Map("sort" -> ""))(AvailableSortingFields('one))

      sorting mustBe 'isLeft
    }

    "return error if sorting is unparseable" in {
      forAll { (sort: String) =>
        whenever(!sort.matches("""-?\w+(,-?\w+)*""")) {
          val sorting = Sorting.create(Map("sort" -> sort))(AvailableSortingFields('one))

          sorting mustBe 'isLeft
        }
      }
    }

    "return error if sorting field not supported" in {
      val availableFields = Set('one, 'two, 'three, 'four)
      forAll { (fields: Seq[Symbol]) =>
        whenever(fields.exists(!availableFields.contains(_))) {
          val queryString = Map("sort" -> fields.map(_.name).mkString(","))
          val sorting = Sorting.create(queryString)(AvailableSortingFields(availableFields))
          sorting mustBe 'isLeft
        }
      }
    }
  }
}
