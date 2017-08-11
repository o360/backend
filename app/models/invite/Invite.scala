package models.invite

import java.sql.Timestamp

import utils.TimestampConverter

/**
  * Invite model.
  */
case class Invite(
  code: String,
  email: String,
  groupIds: Set[Long],
  activationTime: Option[Timestamp],
  creationTime: Timestamp
)

object Invite {
  def apply(email: String, groupIds: Set[Long]): Invite = Invite(
    "",
    email,
    groupIds,
    None,
    TimestampConverter.now
  )
}
