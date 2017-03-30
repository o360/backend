package utils.generator

import models.user.{Role, Status, User}
import org.scalacheck.{Arbitrary, Gen}

/**
  * User's generator for scalacheck.
  */
trait UserGenerator {

  implicit val userArbitrary = Arbitrary {
    for {
      name <- Arbitrary.arbitrary[Option[String]]
      email <- Arbitrary.arbitrary[Option[String]]
      role <- Gen.oneOf(Role.User, Role.Admin)
      status <- Gen.oneOf(Status.New, Status.Approved)
    } yield User(0, name, email, role, status)
  }

}
