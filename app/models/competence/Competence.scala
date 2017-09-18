package models.competence

import models.NamedEntity

/**
  * Competence.
  */
case class Competence(
  id: Long,
  groupId: Long,
  name: String,
  description: Option[String]
) {
  def toNamedEntity = NamedEntity(id, name)
}

object Competence {
  val namePlural = "competence"
}
