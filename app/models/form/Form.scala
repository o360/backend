package models.form

import models.NamedEntity
import models.form.element.ElementKind

/**
  * Form template model.
  */
case class Form(
  id: Long,
  name: String,
  elements: Seq[Form.Element],
  kind: Form.Kind,
  showInAggregation: Boolean,
  machineName: String
) {

  /**
    * Returns short form.
    */
  def toShort = FormShort(id, name, kind, showInAggregation, machineName)
}

object Form {
  val nameSingular = "form"

  /**
    * Form element.
    */
  case class Element(
    id: Long,
    kind: ElementKind,
    caption: String,
    required: Boolean,
    values: Seq[ElementValue],
    competencies: Seq[ElementCompetence],
    machineName: String
  )

  /**
    * Form element value.
    */
  case class ElementValue(
    id: Long,
    caption: String,
    competenceWeight: Option[Double] = None
  )

  /**
    * Form kind.
    */
  sealed trait Kind
  object Kind {

    /**
      * Active form used as template. Active forms can be listed, edited, deleted.
      */
    case object Active extends Kind

    /**
      * Freezed form. Freezed form is a copy of active form assigned to event.
      */
    case object Freezed extends Kind
  }

  /**
    * Element competence.
    */
  case class ElementCompetence(
    competence: NamedEntity,
    factor: Double
  )
}
