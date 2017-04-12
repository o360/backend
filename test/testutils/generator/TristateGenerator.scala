package testutils.generator

import org.davidbild.tristate.Tristate
import org.scalacheck.{Arbitrary, Gen}


/**
  * Tristate generator for scalacheck.
  */
trait TristateGenerator {

  implicit def tristateArbitrary[A](implicit arbitrary: Arbitrary[A]): Arbitrary[Tristate[A]] = Arbitrary {
    for {
      value <- Arbitrary.arbitrary[A]
      result <- Gen.oneOf(Tristate.Unspecified, Tristate.Absent, Tristate.present(value))
    } yield result
  }
}
