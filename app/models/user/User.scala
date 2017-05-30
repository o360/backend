package models.user

import com.mohiva.play.silhouette.api.Identity
import silhouette.CustomSocialProfile

/**
  * User model.
  *
  * @param id     DB ID
  * @param name   full name
  * @param email  email
  * @param gender gender
  * @param role   role
  * @param status status
  */
case class User(
  id: Long,
  name: Option[String],
  email: Option[String],
  gender: Option[User.Gender],
  role: User.Role,
  status: User.Status
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
      User.Status.New
    )
  }
}
