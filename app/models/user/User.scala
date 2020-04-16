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

package models.user

import java.time.{ZoneId, ZoneOffset}

import com.mohiva.play.silhouette.api.Identity
import silhouette.CustomSocialProfile

/**
  * User model.
  */
case class User(
  id: Long,
  firstName: Option[String],
  lastName: Option[String],
  email: Option[String],
  gender: Option[User.Gender],
  role: User.Role,
  status: User.Status,
  timezone: ZoneId,
  termsApproved: Boolean,
  pictureName: Option[String]
) extends Identity {

  def fullName: Option[String] = (firstName, lastName) match {
    case (Some(firstName), Some(lastName)) => Some(lastName + " " + firstName)
    case (Some(firstName), None)           => Some(firstName)
    case (None, Some(lastName))            => Some(lastName)
    case (None, None)                      => None
  }

}

object User {
  val nameSingular = "user"

  /**
    * User's role.
    */
  trait Role
  object Role {

    /**
      * User.
      */
    case object User extends Role

    /**
      * Admin.
      */
    case object Admin extends Role
  }

  /**
    * User's status.
    */
  trait Status
  object Status {

    /**
      * New user.
      */
    case object New extends Status

    /**
      * Approved.
      */
    case object Approved extends Status
  }

  /**
    * User's gender.
    */
  trait Gender
  object Gender {
    case object Male extends Gender
    case object Female extends Gender
  }

  /**
    * Creates user model from social profile.
    */
  def fromSocialProfile(socialProfile: CustomSocialProfile): User = {
    User(
      id = 0,
      firstName = socialProfile.firstName,
      lastName = socialProfile.lastName,
      email = socialProfile.email,
      gender = socialProfile.gender,
      role = User.Role.User,
      status = User.Status.New,
      timezone = ZoneOffset.UTC,
      termsApproved = false,
      pictureName = None
    )
  }
}
