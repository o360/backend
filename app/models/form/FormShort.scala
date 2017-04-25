package models.form

/**
  * Short form model, without elements.
  */
case class FormShort(
  id: Long,
  name: String
) {

  /**
    * Returns full form model.
    *
    * @param elements form elements
    */
  def toModel(elements: Seq[Form.Element]) = Form(
    id,
    name,
    elements
  )
}
