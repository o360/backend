package controllers.api.user

import java.time.ZoneId

import models.user.User
import play.api.libs.json.Json

/**
  * Partial user model for self-updates.
  */
case class ApiPartialUser(
  name: Option[String],
  email: Option[String],
  gender: Option[ApiUser.ApiGender],
  timezone: ZoneId,
  termsApproved: Boolean
) {
  def applyTo(user: User): User = {
    user.copy(
      name = name,
      email = email,
      gender = gender.map(_.value),
      timezone = timezone,
      termsApproved = termsApproved
    )
  }
}

object ApiPartialUser {
  implicit val reads = Json.reads[ApiPartialUser]
}
