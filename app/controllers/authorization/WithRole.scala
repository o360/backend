package controllers.authorization

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.user.User
import play.api.mvc.Request

import scala.concurrent.Future

/**
  * Roles authorization. Access is allowed only for given roles.
  */
case class WithRole(roles: User.Role*) extends Authorization[User, JWTAuthenticator] {
  override def isAuthorized[B](
    identity: User, authenticator: JWTAuthenticator
  )(implicit request: Request[B]): Future[Boolean] = {
    Future.successful(roles.contains(identity.role))
  }
}

case object WithRole {
  /**
    * Allows access only to admins.
    */
  def admin = WithRole(User.Role.Admin)
}
