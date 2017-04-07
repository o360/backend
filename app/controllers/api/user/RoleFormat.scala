package controllers.api.user

import controllers.api.{BaseEnumFormat, BaseEnumFormatHelper}
import models.user.User

/**
  * Format for user role.
  */
case class RoleFormat(value: User.Role) extends BaseEnumFormat[User.Role]

object RoleFormat extends BaseEnumFormatHelper[User.Role, RoleFormat]("role") {

  override protected val mapping: Map[String, User.Role] = Map(
    "user" -> User.Role.User,
    "admin" -> User.Role.Admin
  )
}

