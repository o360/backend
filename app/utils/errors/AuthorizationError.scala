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

  case object CompletedEventUpdating
    extends AuthorizationError("AUTHORIZATION-EVENT-2", "Can't update completed event")

  case class Form(id: Long) extends AuthorizationError("AUTHORIZATION-FORM-1", "Can't get form by ID.")
}
