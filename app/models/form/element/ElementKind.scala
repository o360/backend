package models.form.element

/**
  * Kind of form element.
  *
  * @param needValues is list of values needed
  */
sealed abstract class ElementKind(val needValues: Boolean) {
  val isValuesNeeded = needValues
}

case object TextField extends ElementKind(false)
case object TextArea extends ElementKind(false)
case object Checkbox extends ElementKind(false)
case object CheckboxGroup extends ElementKind(true)
case object Radio extends ElementKind(true)
case object Select extends ElementKind(true)
case object LikeDislike extends ElementKind(true)
