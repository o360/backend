package models.invite

import java.time.LocalDateTime

import models.NamedEntity
import utils.TimestampConverter

/**
  * Invite model.
  */
case class Invite(
  code: String,
  email: String,
  groups: Set[NamedEntity],
  activationTime: Option[LocalDateTime],
  creationTime: LocalDateTime
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
