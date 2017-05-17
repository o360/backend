package controllers.api.user

import models.user.UserShort
import play.api.libs.json.Json

/**
  * Short user API model.
  */
case class ApiShortUser(
  id: Long,
  name: String,
  gender: ApiUser.ApiGender
)

object ApiShortUser {

  implicit val shortUserWrites = Json.writes[ApiShortUser]

  def apply(user: UserShort): ApiShortUser = ApiShortUser(
    user.id,
    user.name,
    ApiUser.ApiGender(user.gender)
  )
}
