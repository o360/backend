package models.competence

/**
  * Competence group.
  */
case class CompetenceGroup(
  id: Long,
  name: String,
  description: Option[String]
)

object CompetenceGroup {
  val nameSingular = "competence-group"
}
