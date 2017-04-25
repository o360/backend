package models.project

/**
  * Project model.
  *
  * @param id           DB iD
  * @param name         prohect name
  * @param description  description
  * @param groupAuditor group-auditor ID
  * @param relations    relations list
  */
case class Project(
  id: Long,
  name: String,
  description: Option[String],
  groupAuditor: Long,
  relations: Seq[Project.Relation]
)

object Project {

  /**
    * Relation inside project.
    *
    * @param groupFrom reviewer group ID
    * @param groupTo   reviewed group ID
    * @param form      form template ID
    * @param kind      relation kind
    */
  case class Relation(
    groupFrom: Long,
    groupTo: Option[Long],
    form: Long,
    kind: RelationKind
  )

  /**
    * Kind of relation.
    */
  sealed trait RelationKind
  object RelationKind {

    /**
      * Group2group.
      */
    case object Classic extends RelationKind

    /**
      * Survey, single group.
      */
    case object Survey extends RelationKind
  }
}
