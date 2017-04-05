package controllers.api.user

import controllers.api.BaseResponse
import models.user.User
import play.api.libs.json.Json

/**
  * User response model.
  *
  * @param id        DB ID
  * @param name      full name
  * @param email     email
  * @param role      role
  */
case class UserResponse(
  id: Long,
  name: Option[String],
  email: Option[String],
  role: String,
  status: String
) extends BaseResponse

object UserResponse {
  implicit val writes = Json.writes[UserResponse]

  /**
    * Converts user to user response.
    *
    * @param user user
    * @return user response
    */
  def apply(user: User): UserResponse = UserResponse(
    user.id,
    user.name,
    user.email,
    user.role.toString,
    user.status.toString
  )
}
