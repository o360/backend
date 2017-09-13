package models.assessment

import models.user.User

/**
  * Assessment object model.
  *
  * @param user  assessed user
  * @param forms forms ids with answers
  */
case class Assessment(
  user: Option[User],
  forms: Seq[Answer]
)
