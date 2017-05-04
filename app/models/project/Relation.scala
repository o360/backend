package models.project

import models.NamedEntity

/**
  * Relation inside project.
  *
  * @param id        DB ID
  * @param project   relation project
  * @param groupFrom reviewer group
  * @param groupTo   reviewed group
  * @param form      form template
  * @param kind      relation kind
  * @param templates relation-wide email templates
  */
case class Relation(
  id: Long,
  project: NamedEntity,
  groupFrom: NamedEntity,
  groupTo: Option[NamedEntity],
  form: NamedEntity,
  kind: Relation.Kind,
  templates: Seq[TemplateBinding]
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
