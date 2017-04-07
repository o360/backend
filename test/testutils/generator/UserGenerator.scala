package testutils.generator

import models.user.User
import org.scalacheck.{Arbitrary, Gen}

/**
  * User's generator for scalacheck.
  */
trait UserGenerator {

  implicit val roleArbitrary = Arbitrary[User.Role] {
    Gen.oneOf(User.Role.User, User.Role.Admin)
  }

  implicit val statusArbitrary = Arbitrary[User.Status] {
    Gen.oneOf(User.Status.New, User.Status.Approved)
  }

  implicit val userArbitrary = Arbitrary {
    for {
      name <- Arbitrary.arbitrary[Option[String]]
      email <- Arbitrary.arbitrary[Option[String]]
      role <- Arbitrary.arbitrary[User.Role]
      status <- Arbitrary.arbitrary[User.Status]
    } yield User(0, name, email, role, status)
  }

}
