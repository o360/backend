package utils.errors

/**
  * Base application error.
  *
  * @param code    error code
  * @param message error message
  */
abstract class ApplicationError(
  code: String,
  message: String
) {
  /**
    * Returns error code.
    */
  def getCode = code

  /**
    * Returns error message.
    */
  def getMessage = message
}


