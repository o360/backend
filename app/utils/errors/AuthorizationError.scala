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

package utils.errors

/**
  * Authorization error.
  */
abstract class AuthorizationError(
  code: String,
  message: String,
  logMessage: Option[String] = None
) extends ApplicationError(code, message, logMessage)

object AuthorizationError {

  case object General extends AuthorizationError("AUTHORIZATION-1", "Not authorized")

  case class FieldUpdate(fields: String, model: String, logMessage: String)
    extends AuthorizationError("AUTHORIZATION-2", s"Can't update field [$fields] in [$model]", Some(logMessage))

  case object ProjectsInEventUpdating
    extends AuthorizationError("AUTHORIZATION-EVENT-1", "Can't add or remove projects from in-progress event")

  case object CompletedEventUpdating extends AuthorizationError("AUTHORIZATION-EVENT-2", "Can't update completed event")

  case class Form(id: Long) extends AuthorizationError("AUTHORIZATION-FORM-1", "Can't get form by ID.")

  object Report {
    case class OnlyAuditor(id: Long)
      extends AuthorizationError("AUTHORIZATION-REPORT-1", s"Only auditors can get report for project $id")
  }
}
