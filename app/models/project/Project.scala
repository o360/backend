package models.project

/**
  * Project model.
  *
  * @param id           DB iD
  * @param name         project name
  * @param description  description
  * @param groupAuditor group-auditor ID
  */
case class Project(
  id: Long,
  name: String,
  description: Option[String],
  groupAuditor: Long
)
