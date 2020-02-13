/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
