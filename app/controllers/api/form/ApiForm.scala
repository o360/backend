package controllers.api.form

import controllers.api.{EnumFormat, EnumFormatHelper, Response}
import models.form.{Form, FormShort}
import play.api.libs.json.Json

/**
  * Api form model.
  *
  * @param id       ID
  * @param name     name
  * @param elements form elements
  */
case class ApiForm(
  id: Option[Long],
  name: String,
  elements: Option[Seq[ApiForm.Element]]
) extends Response {

  def toModel = Form(
    id.getOrElse(0),
    name,
    elements.getOrElse(Nil).map(_.toModel)
  )
}

object ApiForm {

  /**
    * Converts form to ApiForm.
    *
    * @param form form
    */
  def apply(form: Form): ApiForm = ApiForm(
    Some(form.id),
    form.name,
    Some(form.elements.map(Element(_)))
  )

  /**
    * Converts short form to ApiForm.
    *
    * @param form short form
    */
  def apply(form: FormShort): ApiForm = ApiForm(
    Some(form.id),
    form.name,
    None
  )

  implicit val elementValueFormat = Json.format[ElementValue]
  implicit val elementFormat = Json.format[Element]
  implicit val formFormat = Json.format[ApiForm]

  /**
    * Form element api model.
    */
  case class Element(
    kind: ElementKind,
    caption: String,
    defaultValue: Option[String],
    required: Boolean,
    values: Option[Seq[ElementValue]]
  ) extends Response {

    def toModel = Form.Element(
      kind.value,
      caption,
      defaultValue,
      required,
      values.getOrElse(Nil).map(_.toModel)
    )
  }

  object Element {
    /**
      * Converts form element to api form element.
      */
    def apply(element: Form.Element): Element = {
      val values = element.values.map { value =>
        ElementValue(
          value.value,
          value.caption
        )
      }
      Element(
        ElementKind(element.kind),
        element.caption,
        element.defaultValue,
        element.required,
        if (values.isEmpty) None else Some(values)
      )
    }
  }

  /**
    * Element kind api model.
    */
  case class ElementKind(value: Form.ElementKind) extends EnumFormat[Form.ElementKind]
  object ElementKind extends EnumFormatHelper[Form.ElementKind, ElementKind]("element kind") {

    import Form.ElementKind._

    override protected def mapping: Map[String, Form.ElementKind] = Map(
      "textfield" -> TextField,
      "textarea" -> TextArea,
      "checkbox" -> Checkbox,
      "checkboxgroup" -> CheckboxGroup,
      "radio" -> Radio,
      "select" -> Select
    )
  }

  /**
    * Form element value api model.
    */
  case class ElementValue(
    value: String,
    caption: String
  ) extends Response {

    def toModel = Form.ElementValue(
      value,
      caption
    )
  }
}
