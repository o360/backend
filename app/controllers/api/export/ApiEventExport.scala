package controllers.api.export

import controllers.api.Response
import controllers.api.competence.{ApiCompetence, ApiCompetenceGroup}
import controllers.api.form.ApiForm
import controllers.api.user.ApiShortUser
import play.api.libs.json.Json

/**
  * API model for export event results.
  */
case class ApiEventExport(
  forms: Seq[ApiForm],
  users: Seq[ApiShortUser],
  answers: Seq[ApiAnswerExport],
  competencies: Seq[ApiCompetence],
  competenceGroups: Seq[ApiCompetenceGroup]
) extends Response

object ApiEventExport {
  implicit val writes = Json.writes[ApiEventExport]
}
