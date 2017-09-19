package controllers.api.competence

import controllers.api.Response
import models.competence.Competence
import play.api.libs.json.Json
import io.scalaland.chimney.dsl._

/**
  * Competence API model.
  */
case class ApiCompetence(
  id: Long,
  groupId: Long,
  name: String,
  description: Option[String],
  machineName: String
) extends Response

object ApiCompetence {
  implicit val writes = Json.writes[ApiCompetence]

  def fromModel(c: Competence) = c.transformInto[ApiCompetence]
}
