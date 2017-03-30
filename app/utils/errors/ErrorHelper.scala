package utils.errors

import controllers.api.ErrorResponse
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results._

/**
  * Error helper companion.
  */
object ErrorHelper {

  /**
    * Returns play's result with appropriate status code.
    *
    * @param error application error
    */
  def getResult(error: ApplicationError): Result = {
    val statusCode = error match {
      case _: AuthenticationError => Unauthorized
      case _: NotFoundError => NotFound
    }
    val errorResponse = ErrorResponse(
      code = error.getCode,
      message = error.getMessage
    )
    statusCode(Json.toJson(errorResponse))
  }

}
