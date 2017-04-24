package controllers.api.user

import controllers.api.{EnumFormat, EnumFormatHelper, Response}
import models.user.User
import models.user.User.Status
import play.api.libs.json.Json

/**
  * User format model.
  *
  * @param id     DB ID
  * @param name   full name
  * @param email  email
  * @param gender gender
  * @param role   role
  * @param status status
  */
case class ApiUser(
  id: Long,
  name: Option[String],
  email: Option[String],
  gender: Option[ApiUser.ApiGender],
  role: ApiUser.ApiRole,
  status: ApiUser.ApiStatus
) extends Response {

  def toModel = User(
    id = id,
    name = name,
    email = email,
    gender = gender.map(_.value),
    role = role.value,
    status = status.value
  )
}

object ApiUser {
  implicit val writes = Json.format[ApiUser]

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
    user.gender.map(ApiGender(_)),
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

  /**
    * Format for user gender.
    */
  case class ApiGender(value: User.Gender) extends EnumFormat[User.Gender]
  object ApiGender extends EnumFormatHelper[User.Gender, ApiGender]("gender") {

    override protected def mapping: Map[String, User.Gender] = Map(
      "male" -> User.Gender.Male,
      "female" -> User.Gender.Female
    )
  }
}
