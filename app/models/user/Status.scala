package models.user

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
