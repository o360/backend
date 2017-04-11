package controllers

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.api.Response
import models.user.{User => UserModel}
import play.api.libs.json.{Json, Writes}
import play.api.mvc._
import silhouette.DefaultEnv
import utils.errors.{ApplicationError, ErrorHelper}
import utils.listmeta.actions.ListRequest


/**
  * Base class for controllers.
  */
trait BaseController extends Controller {

  /**
    * Converts given value to JSON and returns Ok result.
    *
    * @param value  value to return
    * @param writes writes to convert value to JSON
    */
  def toResult[T <: Response](value: T)(implicit writes: Writes[T]): Result = {
    Ok(Json.toJson(value))
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
    * @param res    either result or error
    * @param writes writes to convert result to JSON
    */
  def toResult[E <: ApplicationError, T <: Response](res: Either[E, T])(implicit writes: Writes[T]): Result = {
    res match {
      case Left(error) => toResult(error)
      case Right(data) => toResult(data)
    }
  }

  /**
    * Converts given value to either error or no content result.
    *
    * @param res either no content or error
    */
  def toResult[E <: ApplicationError](res: Either[E, Response.NoContent.type]): Result = res match {
    case Left(error) => toResult(error)
    case Right(_) => NoContent
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
