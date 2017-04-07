package controllers.api.user

import controllers.api.{BaseEnumFormat, BaseEnumFormatHelper}
import models.user.User
import models.user.User.Status

/**
  * Format for user status.
  */
case class StatusFormat(value: User.Status) extends BaseEnumFormat[User.Status]

object StatusFormat extends BaseEnumFormatHelper[User.Status, StatusFormat]("status") {

  override protected val mapping: Map[String, Status] = Map(
      "new" -> User.Status.New,
      "approved" -> User.Status.Approved
    )
}
