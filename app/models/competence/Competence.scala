package models.competence

import models.{EntityKind, NamedEntity}

/**
  * Competence.
  */
case class Competence(
  id: Long,
  groupId: Long,
  name: String,
  description: Option[String],
  kind: EntityKind
) {
  def toNamedEntity = NamedEntity(id, name)
}

object Competence {
  val namePlural = "competence"
}
