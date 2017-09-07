package models.invite

import java.sql.Timestamp

import models.NamedEntity
import utils.TimestampConverter

/**
  * Invite model.
  */
case class Invite(
  code: String,
  email: String,
  groups: Set[NamedEntity],
  activationTime: Option[Timestamp],
  creationTime: Timestamp
)

object Invite {
  def apply(email: String, groupIds: Set[NamedEntity]): Invite = Invite(
    "",
    email,
    groupIds,
    None,
    TimestampConverter.now
  )
}
