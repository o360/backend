package testutils.generator

import java.time.ZoneOffset

import models.user.{User, UserShort}
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

  implicit val genderArb = Arbitrary[User.Gender] {
    Gen.oneOf(User.Gender.Male, User.Gender.Female)
  }

  implicit val userArbitrary = Arbitrary {
    for {
      id <- Arbitrary.arbitrary[Long]
      name <- Arbitrary.arbitrary[Option[String]]
      email <- Arbitrary.arbitrary[Option[String]]
      gender <- Arbitrary.arbitrary[Option[User.Gender]]
      role <- Arbitrary.arbitrary[User.Role]
      status <- Arbitrary.arbitrary[User.Status]
      termsApproved <- Arbitrary.arbitrary[Boolean]
      pictureName <- Arbitrary.arbitrary[Option[String]]
    } yield User(id, name, email, gender, role, status, ZoneOffset.UTC, termsApproved, pictureName)
  }

  implicit val userShortArb = Arbitrary {
    for {
      id <- Arbitrary.arbitrary[Long]
      name <- Arbitrary.arbitrary[String]
      gender <- Arbitrary.arbitrary[User.Gender]
      hasPicture <- Arbitrary.arbitrary[Boolean]
    } yield UserShort(id, name, gender, hasPicture)
  }

}
