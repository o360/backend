package utils.errors

/**
  * Base application error.
  *
  * @param code       error code
  * @param message    error message
  * @param logMessage log message
  */
abstract class ApplicationError(code: String, message: String, logMessage: Option[String] = None) {
  /**
    * Returns error code.
    */
  def getCode = code

  /**
    * Returns error message.
    */
  def getMessage = message

  /**
    * Returns log message.
    */
  def getLogMessage = logMessage
}


