package silhouette

import com.mohiva.play.silhouette.api.{AuthInfo, LoginInfo}
import com.mohiva.play.silhouette.impl.providers.{SocialProfile, SocialProfileParser}
import models.user.User

/**
  * Custom silhouette social profile.
  */
case class CustomSocialProfile (
  loginInfo: LoginInfo,
  fullName: Option[String],
  email: Option[String],
  gender: Option[User.Gender]
) extends SocialProfile


/**
  * Profile parser for custom social profile.
  */
trait CustomSocialProfileParser[C, A <: AuthInfo] extends SocialProfileParser[C, CustomSocialProfile, A]
