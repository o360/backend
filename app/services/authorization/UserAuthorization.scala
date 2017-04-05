package services.authorization

import models.user.User
import utils.errors.AuthorizationError

/**
  * Authorization rules for users.
  */
object UserAuthorization {

  /**
    * Checks authorization for user updating.
    *
    * @param original original user model
    * @param draft    updated user model
    * @param account  logged in user
    * @return left if authorization fails, right otherwise
    */
  def canUpdate(original: User, draft: User)(implicit account: User): Either[AuthorizationError, Unit] = {
    val rules = getValidationRules(original, draft)
    ValidationRule.validate(rules) match {
      case None => Right(())
      case Some(errorMessage) =>
        val logMessage = s"original: $original; draft: $draft; account: $account"
        Left(AuthorizationError.FieldUpdate(errorMessage, "user", logMessage))
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
    ValidationRule("id", original.id != draft.id) {
      false
    },
    ValidationRule("name, email", original.name != draft.name || original.email != draft.email) {
      (draft.status == User.Status.New || draft.name.isDefined && draft.email.isDefined) &&
        (account.role == User.Role.Admin || account.role == User.Role.User && account.id == original.id)
    },
    ValidationRule("role", original.role != draft.role) {
      account.role == User.Role.Admin &&
        draft.status == User.Status.Approved
    },
    ValidationRule("status", original.status != draft.status) {
      account.role == User.Role.Admin &&
        draft.name.isDefined && draft.email.isDefined
    }
  )
}
