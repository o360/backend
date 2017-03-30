package utils.errors

/**
  * Not found error.
  */
abstract class NotFoundError(
  code: String,
  message: String
) extends ApplicationError(code, message)
