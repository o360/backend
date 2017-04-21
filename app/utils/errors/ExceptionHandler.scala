package utils.errors

import java.sql.SQLException

/**
  * Exception handlers.
  */
object ExceptionHandler {

  /**
    * Handle sql exceptions.
    */
  val sql: PartialFunction[Throwable, ApplicationError] = {
    case e: SQLException if e.getSQLState.startsWith("23") => // integrity violation
      ConflictError.General(e.getMessage)
  }
}
