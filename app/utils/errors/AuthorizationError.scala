package utils.errors

/**
  * Authorization error.
  */
abstract class AuthorizationError(
  code: String,
  message: String
) extends ApplicationError(code, message)

object AuthorizationError {

  case object General extends AuthorizationError("GENERAL_AUTHORIZATION", "Not authorized")

}
