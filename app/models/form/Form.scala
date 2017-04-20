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
    * @param kind         type of element(textbox, radio, ...)
    * @param caption      caption
    * @param defaultValue default value
    * @param required     is element required
    * @param values       list of element values
    */
  case class Element(
    kind: ElementKind,
    caption: String,
    defaultValue: Option[String],
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
    * @param value   value
    * @param caption caption
    */
  case class ElementValue(
    value: String,
    caption: String
  )
}
