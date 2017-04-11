package controllers.authorization

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.user.User
import play.api.mvc.Request

import scala.concurrent.Future

/**
  * Roles authorization. Access is allowed only for given roles.
  */
case class AllowedRole(roles: User.Role*) extends Authorization[User, JWTAuthenticator] {
  override def isAuthorized[B](
    identity: User, authenticator: JWTAuthenticator
  )(implicit request: Request[B]): Future[Boolean] = {
    Future.successful(roles.contains(identity.role))
  }
}

case object AllowedRole {
  /**
    * Allows access only to admins.
    */
  def admin = AllowedRole(User.Role.Admin)
}
