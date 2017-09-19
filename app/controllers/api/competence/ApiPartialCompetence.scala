package controllers.api.competence

import io.scalaland.chimney.dsl._
import models.EntityKind
import models.competence.Competence
import play.api.libs.json.Json
import utils.RandomGenerator

/**
  * Partial competence API model.
  */
case class ApiPartialCompetence(
  groupId: Long,
  name: String,
  description: Option[String],
  machineName: Option[String]
) {
  def toModel(id: Long = 0) =
    this
      .into[Competence]
      .withFieldConst(_.id, id)
      .withFieldConst(_.kind, EntityKind.Template: EntityKind)
      .withFieldComputed(_.machineName, _.machineName.getOrElse(RandomGenerator.generateMachineName))
      .transform
}

object ApiPartialCompetence {
  implicit val reads = Json.reads[ApiPartialCompetence]
}
