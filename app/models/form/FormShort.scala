package models.form

/**
  * Short form model, without elements.
  */
case class FormShort(
  id: Long,
  name: String,
  kind: Form.Kind
) {

  /**
    * Returns full form model.
    *
    * @param elements form elements
    */
  def withElements(elements: Seq[Form.Element]) = Form(
    id,
    name,
    elements,
    kind
  )
}
