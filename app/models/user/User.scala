package models.user

import com.mohiva.play.silhouette.api.Identity

/**
  * User model.
  *
  * @param id     DB ID
  * @param name   full name
  * @param email  email
  * @param role   role
  * @param status status
  */
case class User(
  id: Long,
  name: Option[String],
  email: Option[String],
  role: Role,
  status: Status
) extends Identity
