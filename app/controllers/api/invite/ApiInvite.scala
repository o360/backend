package controllers.api.invite

import java.time.LocalDateTime

import controllers.api.ApiNamedEntity
import models.invite.Invite
import models.user.User
import play.api.libs.json.Json
import utils.TimestampConverter

/**
  * API invite model.
  */
case class ApiInvite(
  code: String,
  email: String,
  groups: Set[ApiNamedEntity],
  creationTime: LocalDateTime,
  activationTime: Option[LocalDateTime]
)

object ApiInvite {
  def apply(invite: Invite)(implicit account: User): ApiInvite = ApiInvite(
    invite.code,
    invite.email,
    invite.groups.map(ApiNamedEntity(_)),
    TimestampConverter.fromUtc(invite.creationTime, account.timezone),
    invite.activationTime.map(TimestampConverter.fromUtc(_, account.timezone))
  )

  implicit val writes = Json.writes[ApiInvite]
}
