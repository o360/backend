package models.user

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
