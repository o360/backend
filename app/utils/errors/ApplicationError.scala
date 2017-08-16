package utils.errors

/**
  * Base application error.
  *
  * @param code       error code
  * @param message    error message
  * @param logMessage log message
  * @param inner      inner errors
  */
abstract class ApplicationError(
  code: String,
  message: String,
  logMessage: Option[String] = None,
  inner: Option[Seq[ApplicationError]] = None
) {

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

  /**
    * Returns inner errors.
    */
  def getInnerErrors = inner
}
