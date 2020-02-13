/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
