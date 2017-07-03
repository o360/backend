package utils.errors

import controllers.api.{ApiNamedEntity, Response}
import controllers.api.Response.Error
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results._
import utils.Logger

/**
  * Error helper companion.
  */
object ErrorHelper extends Logger {

  /**
    * Returns play's result with appropriate status code.
    *
    * @param error application error
    */
  def getResult(error: ApplicationError): Result = {
    val statusCode = error match {
      case _: AuthenticationError => Unauthorized
      case _: BadRequestError => BadRequest
      case _: NotFoundError => NotFound
      case _: AuthorizationError => Forbidden
      case _: ConflictError => Conflict
    }

    val errorResponse = getApiError(error)

    val logMessage = error.getLogMessage
    if(logMessage.isDefined) {
      log.debug(s"[${error.getCode}] ${error.getMessage} $logMessage")
    }

    statusCode(Json.toJson(errorResponse))
  }

  private def getApiError(error: ApplicationError): Error = {
    val additionalInfo = error match {
      case e: BadRequestError.Assessment.WithUserFormInfo =>
        Some(Error.AdditionalInfo.UserFormInfo(e.userId, e.formId))
      case e: ConflictError =>
        e.getRelated.map { relatedEntities =>
          Error.AdditionalInfo.ConflictDependencies(relatedEntities.mapValues(_.map(ApiNamedEntity(_))))
        }
      case _ => None
    }

    Error(
      code = error.getCode,
      message = error.getMessage,
      additionalInfo = additionalInfo,
      inner = error.getInnerErrors.map(_.map(getApiError))
    )
  }

}
