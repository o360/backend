package controllers.api.user

import controllers.api.BaseResponse
import models.user.User
import play.api.libs.json.Json

/**
  * User format model.
  *
  * @param id     DB ID
  * @param name   full name
  * @param email  email
  * @param role   role
  * @param status status
  */
case class UserFormat(
  id: Long,
  name: Option[String],
  email: Option[String],
  role: RoleFormat,
  status: StatusFormat
) extends BaseResponse {
  def toUser: User = {
    User(
      id,
      name,
      email,
      role.value,
      status.value
    )
  }
}

object UserFormat {
  implicit val writes = Json.format[UserFormat]

  /**
    * Converts user to user response.
    *
    * @param user user
    * @return user response
    */
  def apply(user: User): UserFormat = UserFormat(
    user.id,
    user.name,
    user.email,
    RoleFormat(user.role),
    StatusFormat(user.status)
  )
}
