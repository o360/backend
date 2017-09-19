package controllers.api.competence
import io.scalaland.chimney.dsl._
import models.EntityKind
import models.competence.CompetenceGroup
import play.api.libs.json.Json
import utils.RandomGenerator

/**
  * Partial competence group API model.
  */
case class ApiPartialCompetenceGroup(
  name: String,
  description: Option[String],
  machineName: Option[String]
) {
  def toModel(id: Long = 0) =
    this
      .into[CompetenceGroup]
      .withFieldConst(_.id, id)
      .withFieldConst(_.kind, EntityKind.Template: EntityKind)
      .withFieldComputed(_.machineName, _.machineName.getOrElse(RandomGenerator.generateMachineName))
      .transform
}

object ApiPartialCompetenceGroup {
  implicit val reads = Json.reads[ApiPartialCompetenceGroup]
}
