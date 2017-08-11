package controllers.api.invite

import java.time.LocalDateTime

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
  groupIds: Set[Long],
  creationTime: LocalDateTime,
  activationTime: Option[LocalDateTime]
)

object ApiInvite {
  def apply(invite: Invite)(implicit account: User): ApiInvite = ApiInvite(
    invite.code,
    invite.email,
    invite.groupIds,
    TimestampConverter.fromUtc(invite.creationTime, account.timezone).toLocalDateTime,
    invite.activationTime.map(TimestampConverter.fromUtc(_, account.timezone).toLocalDateTime)
  )

  implicit val writes = Json.writes[ApiInvite]
}
