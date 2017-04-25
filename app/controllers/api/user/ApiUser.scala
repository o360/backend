package controllers.api.user

import controllers.api.{EnumFormat, EnumFormatHelper, Response}
import models.user.User
import models.user.User.Status
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
/**
  * User format model.
  *
  * @param id     DB ID
  * @param name   full name
  * @param email  email
  * @param role   role
  * @param status status
  */
case class ApiUser(
  id: Long,
  name: Option[String],
  email: Option[String],
  role: ApiUser.ApiRole,
  status: ApiUser.ApiStatus
) extends Response {

  def toModel = User(
    id = id,
    name = name,
    email = email,
    role = role.value,
    status = status.value
  )
}

object ApiUser {
  private val reads: Reads[ApiUser] = (
    (__ \ "id").read[Long] and
      (__ \ "name").readNullable[String](maxLength[String](1024)) and
      (__ \ "email").readNullable[String](maxLength[String](255)) and
      (__ \ "role").read[ApiUser.ApiRole] and
      (__ \ "status").read[ApiUser.ApiStatus]
    ) (ApiUser(_, _, _, _, _))

  implicit val format = Format(reads, Json.writes[ApiUser])

  /**
    * Converts user to user response.
    *
    * @param user user
    * @return user response
    */
  def apply(user: User): ApiUser = ApiUser(
    user.id,
    user.name,
    user.email,
    ApiRole(user.role),
    ApiStatus(user.status)
  )

  /**
    * Format for user role.
    */
  case class ApiRole(value: User.Role) extends EnumFormat[User.Role]
  object ApiRole extends EnumFormatHelper[User.Role, ApiRole]("role") {

    override protected val mapping: Map[String, User.Role] = Map(
      "user" -> User.Role.User,
      "admin" -> User.Role.Admin
    )
  }

  /**
    * Format for user status.
    */
  case class ApiStatus(value: User.Status) extends EnumFormat[User.Status]
  object ApiStatus extends EnumFormatHelper[User.Status, ApiStatus]("status") {

    override protected val mapping: Map[String, Status] = Map(
      "new" -> User.Status.New,
      "approved" -> User.Status.Approved
    )
  }
}
