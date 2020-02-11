package services.authorization

import models.user.User
import utils.errors.AuthorizationError

/**
  * Authorization rules for users.
  */
object UserSda {

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
    ValidationRule(
      "name, email, gender",
      original.name != draft.name || original.email != draft.email || original.gender != draft.gender
    ) {
      draft.status == User.Status.New || draft.name.isDefined && draft.email.isDefined && draft.gender.isDefined
    },
    ValidationRule("role", original.role != draft.role) {
      account.role == User.Role.Admin && (
        draft.status match {
          case User.Status.Approved => true
          case User.Status.New      => draft.role == User.Role.User
        }
      )
    },
    ValidationRule("status", original.status != draft.status) {
      account.role == User.Role.Admin &&
      (draft.status match {
        case User.Status.New      => draft.role != User.Role.Admin
        case User.Status.Approved => draft.name.isDefined && draft.email.isDefined && draft.gender.isDefined
      })
    }
  )
}
