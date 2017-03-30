package utils.errors

/**
  * Bad request error.
  */
abstract class BadRequestError(
  code: String,
  message: String
) extends ApplicationError(code, message)
