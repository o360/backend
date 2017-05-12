package testutils.generator

import models.ListWithTotal
import org.scalacheck.{Arbitrary, Gen}

/**
  * ListWithTotal generator for scalacheck.
  */
trait ListWithTotalGenerator extends NotificationGenerator {

  implicit def listWithTotalArb[A](implicit arbitrary: Arbitrary[A]): Arbitrary[ListWithTotal[A]] = Arbitrary {
    for {
      values <- Arbitrary.arbitrary[Seq[A]]
      total <- Gen.choose(values.length, Int.MaxValue)
    } yield ListWithTotal(total, values)
  }
}
