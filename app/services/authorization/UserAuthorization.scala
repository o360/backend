package services.authorization

import models.user.User
import utils.errors.AuthorizationError

/**
  * Authorization rules for users.
  */
object UserAuthorization {

  /**
    * Checks authorization for getting by ID.
    *
    * @param id      user ID
    * @param account logged in user
    * @return some error if authorization failed
    */
  def canGetById(id: Long)(implicit account: User): Option[AuthorizationError] = account.role match {
    case User.Role.Admin => None
    case User.Role.User if account.id == id => None
    case _ => Some(AuthorizationError.General)
  }

  /**
    * Checks authorization for user updating.
    *
    * @param original original user model
    * @param draft    updated user model
    * @param account  logged in user
    * @return some error if authorization failed
    */
  def canUpdate(original: User, draft: User)(implicit account: User): Option[AuthorizationError] = {
    if (account.role == User.Role.Admin || account.role == User.Role.User && draft.id == account.id) {
      val rules = getValidationRules(original, draft)
      ValidationRule.validate(rules) match {
        case None => None
        case Some(errorMessage) =>
          val logMessage = s"original: $original; draft: $draft; account: $account"
          Some(AuthorizationError.FieldUpdate(errorMessage, "user", logMessage))
      }
    } else {
      Some(AuthorizationError.General)
    }
  }

  /**
    * Returns validation rules for user model updating.
    *
    * @param original original user model
    * @param draft    updated user model
    * @param account  logged in user
    */
  private def getValidationRules(original: User, draft: User)(implicit account: User) = Seq(
    ValidationRule("name, email", original.name != draft.name || original.email != draft.email) {
      draft.status == User.Status.New || draft.name.isDefined && draft.email.isDefined
    },
    ValidationRule("role", original.role != draft.role) {
      account.role == User.Role.Admin && (
        draft.status match {
          case User.Status.Approved => true
          case User.Status.New => draft.role == User.Role.User
        }
        )
    },
    ValidationRule("status", original.status != draft.status) {
      account.role == User.Role.Admin &&
        (draft.status match {
          case User.Status.New => draft.role != User.Role.Admin
          case User.Status.Approved => draft.name.isDefined && draft.email.isDefined
        })
    }
  )
}
