package models.assessment

import models.user.UserShort

/**
  * Assessment object model.
  *
  * @param user assessed user
  * @param formIds form ids
  */
case class Assessment(
  user: Option[UserShort],
  formIds: Seq[Long]
)


