package silhouette

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import models.user.{User => UserModel}

/**
  * Environment for silhouette.
  */
trait DefaultEnv extends Env {
  type I = UserModel
  type A = JWTAuthenticator
}
