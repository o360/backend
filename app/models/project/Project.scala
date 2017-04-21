package models.project

/**
  * Project model.
  *
  * @param id          DB iD
  * @param name        prohect name
  * @param description description
  * @param relations   relations list
  */
case class Project(
  id: Long,
  name: String,
  description: Option[String],
  relations: Seq[Project.Relation]
)

object Project {

  /**
    * Relation inside project.
    *
    * @param groupFrom    reviewer group ID
    * @param groupTo      reviewed group ID
    * @param groupAuditor group-auditor ID
    * @param form         form template ID
    */
  case class Relation(
    groupFrom: Long,
    groupTo: Long,
    groupAuditor: Long,
    form: Long
  )
}
