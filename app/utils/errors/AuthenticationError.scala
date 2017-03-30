package utils.errors

/**
  * Authentication error.
  */
abstract class AuthenticationError(
  code: String,
  message: String
) extends ApplicationError(code, message)

object AuthenticationError {
  case class ProviderNotSupported(providerName: String) extends AuthenticationError("PROVIDER_NOT_SUPPORTED", s"Social provider $providerName not supported")

  case object General extends AuthenticationError("GENERAL_AUTHENTICATION", "Not authenticated")
}

