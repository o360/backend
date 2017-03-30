package silhouette

import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import play.api.mvc.{RequestHeader, Result}
import utils.errors.{AuthenticationError, AuthorizationError, ErrorHelper}

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Custom error handler for silhouette errors.
  */
class SilhouetteErrorHandler extends SecuredErrorHandler {
  override def onNotAuthorized(implicit request: RequestHeader): Future[Result] = async {
    ErrorHelper.getResult(AuthorizationError.General)
  }

  override def onNotAuthenticated(implicit request: RequestHeader): Future[Result] = async {
    ErrorHelper.getResult(AuthenticationError.General)
  }
}
