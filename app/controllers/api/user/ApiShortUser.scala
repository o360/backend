package controllers.api.user

import controllers.api.Response
import models.user.UserShort
import play.api.libs.json.Json
import io.scalaland.chimney.dsl._

/**
  * Short user API model.
  */
case class ApiShortUser(
  id: Long,
  name: String,
  gender: ApiUser.ApiGender,
  hasPicture: Boolean
) extends Response

object ApiShortUser {

  implicit val shortUserWrites = Json.writes[ApiShortUser]

  def apply(user: UserShort): ApiShortUser =
    user
      .into[ApiShortUser]
      .withFieldComputed(_.gender, x => ApiUser.ApiGender(x.gender))
      .transform
}
