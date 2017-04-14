package utils

import javax.inject.Singleton

import controllers.api.Response.Error
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import utils.implicits.FutureLifting._

import scala.concurrent.Future

/**
  * Custom error handler.
  */
@Singleton
class ErrorHandler extends HttpErrorHandler with Logger {
  private val statusCodesMapping = Map(
    415 -> 400 // wrong json format now is bad request
  )

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    val code = statusCodesMapping.getOrElse(statusCode, statusCode)
    Status(code)(Json.toJson(Error("GENERAL-1", message))).toFuture
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    log.error("Server error", exception)
    InternalServerError(Json.toJson(Error("GENERAL-2", "Server error"))).toFuture
  }
}
