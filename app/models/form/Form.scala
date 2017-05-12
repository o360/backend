package models.form

/**
  * Form template model.
  *
  * @param id       DB ID
  * @param name     form name
  * @param elements form elements
  */
case class Form(
  id: Long,
  name: String,
  elements: Seq[Form.Element]
) {

  /**
    * Returns short form.
    */
  def toShort = FormShort(id, name)
}

object Form {

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
}
