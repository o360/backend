package models.competence

import models.EntityKind

/**
  * Competence group.
  */
case class CompetenceGroup(
  id: Long,
  name: String,
  description: Option[String],
  kind: EntityKind,
  machineName: String
)

object CompetenceGroup {
  val nameSingular = "competence-group"
}
