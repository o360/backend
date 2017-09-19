package controllers.api.competence

import controllers.api.Response
import play.api.libs.json.Json
import io.scalaland.chimney.dsl._
import models.competence.CompetenceGroup

/**
  * Competence group API model.
  */
case class ApiCompetenceGroup(
  id: Long,
  name: String,
  description: Option[String],
  machineName: String
) extends Response

object ApiCompetenceGroup {
  implicit val writes = Json.writes[ApiCompetenceGroup]

  def fromModel(cg: CompetenceGroup) = cg.transformInto[ApiCompetenceGroup]
}
