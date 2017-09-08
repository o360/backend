package controllers.api.user

import controllers.api.Response
import models.user.UserShort
import play.api.libs.json.Json

/**
  * Short user API model.
  */
case class ApiShortUser(
  id: Long,
  name: String,
  gender: ApiUser.ApiGender
) extends Response

object ApiShortUser {

  implicit val shortUserWrites = Json.writes[ApiShortUser]

  def apply(user: UserShort): ApiShortUser = ApiShortUser(
    user.id,
    user.name,
    ApiUser.ApiGender(user.gender)
  )
}
