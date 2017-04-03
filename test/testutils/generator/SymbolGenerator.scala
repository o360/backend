package testutils.generator

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary

/**
  * Symbol generator for scalacheck.
  */
trait SymbolGenerator {

  implicit lazy val symbolArbitrary = Arbitrary(arbitrary[String].map(Symbol(_)))
}
