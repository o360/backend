package controllers.api.invite

import models.NamedEntity
import models.invite.Invite
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
  * API invite model.
  */
case class ApiPartialInvite(
  email: String,
  groups: Set[Long]
) {
  def toModel = Invite(email, groups.map(NamedEntity(_)))
}

object ApiPartialInvite {
  implicit val reads: Reads[ApiPartialInvite] = (
    (__ \ "email").read[String](maxLength[String](255)) and
      (__ \ "groups").read[Set[Long]]
  )(ApiPartialInvite(_, _))
}
