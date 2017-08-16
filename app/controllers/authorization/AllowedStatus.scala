package controllers.authorization

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.user.User
import play.api.mvc.Request

import scala.concurrent.Future

/**
  * Status authorization. Access is allowed only for given statuses.
  */
case class AllowedStatus(statuses: User.Status*) extends Authorization[User, JWTAuthenticator] {
  override def isAuthorized[B](
    identity: User,
    authenticator: JWTAuthenticator
  )(implicit request: Request[B]): Future[Boolean] = {
    Future.successful(statuses.contains(identity.status))
  }
}

case object AllowedStatus {

  /**
    * Allows access only to approved users.
    */
  val approved = AllowedStatus(User.Status.Approved)
}
