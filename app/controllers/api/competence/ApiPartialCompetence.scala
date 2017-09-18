package controllers.api.competence

import io.scalaland.chimney.dsl._
import models.competence.Competence
import play.api.libs.json.Json

/**
  * Partial competence API model.
  */
case class ApiPartialCompetence(
  groupId: Long,
  name: String,
  description: Option[String]
) {
  def toModel(id: Long = 0) = this.into[Competence].withFieldConst(_.id, id).transform
}

object ApiPartialCompetence {
  implicit val reads = Json.reads[ApiPartialCompetence]
}
