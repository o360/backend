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
      "firstName, lastName, email, gender",
      original.firstName != draft.firstName || original.lastName != draft.lastName || original.email != draft.email || original.gender != draft.gender
    ) {
      draft.status == User.Status.New || draft.firstName.isDefined && draft.lastName.isDefined && draft.email.isDefined && draft.gender.isDefined
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
        case User.Status.Approved => draft.firstName.isDefined && draft.email.isDefined && draft.gender.isDefined
      })
    }
  )
}
