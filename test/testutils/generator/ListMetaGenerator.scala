package testutils.generator

import org.scalacheck.{Arbitrary, Gen}
import utils.listmeta.ListMeta
import utils.listmeta.pagination.Pagination
import utils.listmeta.sorting.Sorting

/**
  * ListMeta generator for scalacheck.
  */
trait ListMetaGenerator extends SymbolGenerator {

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
        name <- Arbitrary.arbitrary[Symbol]
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
