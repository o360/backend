package controllers.api.invite

import play.api.libs.json.Json

/**
  * Invite code API model.
  */
case class ApiInviteCode(
  code: String
)

object ApiInviteCode {
  implicit val reads = Json.reads[ApiInviteCode]
}
