package testutils.generator

import models.group.Group
import org.scalacheck.Arbitrary

/**
  * Group generator for scalacheck.
  */
trait GroupGenerator {

  implicit val groupArbitrary = Arbitrary {
    for {
      id <- Arbitrary.arbitrary[Long]
      parentId <- Arbitrary.arbitrary[Option[Long]]
      name <- Arbitrary.arbitrary[String]
      hasChildren <- Arbitrary.arbitrary[Boolean]
      level <- Arbitrary.arbitrary[Int]
    } yield Group(id, parentId, name, hasChildren, level)
  }
}
