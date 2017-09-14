package models.project

import models.NamedEntity

/**
  * Relation inside project.
  */
case class Relation(
  id: Long,
  project: NamedEntity,
  groupFrom: NamedEntity,
  groupTo: Option[NamedEntity],
  form: NamedEntity,
  kind: Relation.Kind,
  templates: Seq[TemplateBinding],
  hasInProgressEvents: Boolean,
  canSelfVote: Boolean,
  canSkipAnswers: Boolean
) {
  def toNamedEntity = {
    NamedEntity(id, s"${groupFrom.name.getOrElse("")} -> ${groupTo.flatMap(_.name).getOrElse("...")}")
  }
}

object Relation {
  val namePlural = "relations"

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
