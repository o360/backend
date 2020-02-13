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

package controllers

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.api.Response
import models.user.{User => UserModel}
import play.api.libs.json.{Json, Writes}
import play.api.mvc._
import play.api.mvc.{BaseController => PlayBaseController}
import silhouette.DefaultEnv
import utils.errors.{ApplicationError, ErrorHelper}
import utils.implicits.FutureLifting._
import utils.listmeta.actions.ListRequest

import scala.concurrent.{ExecutionContext, Future}
import scalaz.EitherT

/**
  * Base class for controllers.
  */
trait BaseController extends PlayBaseController {

  implicit val ec: ExecutionContext

  val controllerComponents: ControllerComponents

  /**
    * Converts given value to JSON and returns Ok result.
    *
    * @param value  value to return
    * @param status response status
    * @param writes writes to convert value to JSON
    */
  def toResult[T <: Response](value: T, status: Status = Ok)(implicit writes: Writes[T]): Result = {
    status(Json.toJson(value))
  }

  /**
    * Converts given error to JSON and returns result with appropriate status code.
    *
    * @param error error to return
    */
  def toResult[E <: ApplicationError](error: E): Result = {
    ErrorHelper.getResult(error)
  }

  /**
    * Converts given value to either error or successful result.
    *
    * @param status response status
    * @param res    either result or error
    * @param writes writes to convert result to JSON
    */
  def toResult[E <: ApplicationError, T <: Response](
    status: Status
  )(res: EitherT[Future, E, T])(implicit writes: Writes[T]): Future[Result] = {
    res.fold(
      error => toResult(error),
      data => toResult(data, status)
    )
  }

  /**
    * Extracts user from secured request.
    *
    * @param request secured request
    * @return user
    */
  implicit def request2user(implicit request: SecuredRequest[DefaultEnv, _]): UserModel = request.identity

  /**
    * Extracts user from secured list request.
    *
    * @param request secured list request
    * @return user
    */
  implicit def listRequest2user(implicit request: ListRequest[_]): UserModel = request.inner.identity
}
