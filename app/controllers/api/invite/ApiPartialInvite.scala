package controllers.api.invite

import models.invite.Invite
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
  * API invite model.
  */
case class ApiPartialInvite(
  email: String,
  groupIds: Set[Long]
) {
  def toModel = Invite(email, groupIds)
}

object ApiPartialInvite {
  implicit val reads: Reads[ApiPartialInvite] = (
    (__ \ "email").read[String](maxLength[String](255)) and
      (__ \ "groupIds").read[Set[Long]]
  )(ApiPartialInvite(_, _))
}
