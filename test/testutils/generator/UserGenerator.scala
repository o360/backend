package testutils.generator

import models.user.User
import org.scalacheck.{Arbitrary, Gen}

/**
  * User's generator for scalacheck.
  */
trait UserGenerator {

  implicit val userArbitrary = Arbitrary {
    for {
      name <- Arbitrary.arbitrary[Option[String]]
      email <- Arbitrary.arbitrary[Option[String]]
      role <- Gen.oneOf(User.Role.User, User.Role.Admin)
      status <- Gen.oneOf(User.Status.New, User.Status.Approved)
    } yield User(0, name, email, role, status)
  }

}
