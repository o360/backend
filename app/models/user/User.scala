package models.user

import com.mohiva.play.silhouette.api.Identity

/**
  * User model.
  *
  * @param id     DB ID
  * @param name   full name
  * @param email  email
  * @param role   role
  * @param status status
  */
case class User(
  id: Long,
  name: Option[String],
  email: Option[String],
  role: User.Role,
  status: User.Status
) extends Identity


object User {
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


}