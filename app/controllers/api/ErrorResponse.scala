package controllers.api

import play.api.libs.json.Json

/**
  * Error response model.
  *
  * @param code    error code
  * @param message error message
  */
case class ErrorResponse(
  code: String,
  message: String
) extends BaseResponse

object ErrorResponse {
  implicit val writes = Json.writes[ErrorResponse]
}
