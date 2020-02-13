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
  name: Option[String],
  email: Option[String],
  gender: Option[User.Gender],
  role: User.Role,
  status: User.Status,
  timezone: ZoneId,
  termsApproved: Boolean,
  pictureName: Option[String]
) extends Identity

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
      0,
      socialProfile.fullName,
      socialProfile.email,
      socialProfile.gender,
      User.Role.User,
      User.Status.New,
      ZoneOffset.UTC,
      termsApproved = false,
      None
    )
  }
}
