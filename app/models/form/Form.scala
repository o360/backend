package models.form

/**
  * Form template model.
  *
  * @param id       DB ID
  * @param name     form name
  * @param elements form elements
  * @param kind     kind of the form
  */
case class Form(
  id: Long,
  name: String,
  elements: Seq[Form.Element],
  kind: Form.Kind
) {

  /**
    * Returns short form.
    */
  def toShort = FormShort(id, name, kind)
}

object Form {
  val nameSingular = "form"

  /**
    * Form element.
    *
    * @param id           DB ID
    * @param kind         type of element(textbox, radio, ...)
    * @param caption      caption
    * @param required     is element required
    * @param values       list of element values
    */
  case class Element(
    id: Long,
    kind: ElementKind,
    caption: String,
    required: Boolean,
    values: Seq[ElementValue]
  )

  /**
    * Kind of form element.
    *
    * @param needValues is list of values needed
    */
  sealed abstract class ElementKind(val needValues: Boolean) {
    val isValuesNeeded = needValues
  }
  object ElementKind {
    case object TextField extends ElementKind(false)
    case object TextArea extends ElementKind(false)
    case object Checkbox extends ElementKind(false)
    case object CheckboxGroup extends ElementKind(true)
    case object Radio extends ElementKind(true)
    case object Select extends ElementKind(true)
  }

  /**
    * Form element value.
    *
    * @param id      DB ID
    * @param caption caption
    */
  case class ElementValue(
    id: Long,
    caption: String
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
}
