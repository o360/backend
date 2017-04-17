package silhouette

import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import play.api.mvc.{RequestHeader, Result}
import utils.errors.{AuthenticationError, AuthorizationError, ErrorHelper}
import utils.implicits.FutureLifting._

import scala.concurrent.Future

/**
  * Custom error handler for silhouette errors.
  */
class SilhouetteErrorHandler extends SecuredErrorHandler {
  override def onNotAuthorized(implicit request: RequestHeader): Future[Result] = {
    ErrorHelper.getResult(AuthorizationError.General).toFuture
  }

  override def onNotAuthenticated(implicit request: RequestHeader): Future[Result] = {
    ErrorHelper.getResult(AuthenticationError.General).toFuture
  }
}
