package models.project

/**
  * Relation inside project.
  *
  * @param id        DB ID
  * @param groupFrom reviewer group ID
  * @param groupTo   reviewed group ID
  * @param form      form template ID
  * @param kind      relation kind
  */
case class Relation(
  id: Long,
  projectId: Long,
  groupFrom: Long,
  groupTo: Option[Long],
  form: Long,
    kind: Relation.Kind
)

object Relation {
  /**
    * Kind of relation.
    */
  sealed trait Kind
  object Kind {

    /**
      * Group2group.
      */
    case object Classic extends Kind

    /**
      * Survey, single group.
      */
    case object Survey extends Kind
  }
}
