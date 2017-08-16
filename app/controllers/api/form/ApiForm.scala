package controllers.api.form

import controllers.api.{EnumFormat, EnumFormatHelper, Response}
import models.form.{Form, FormShort}
import play.api.libs.json._

/**
  * Api form model.
  */
case class ApiForm(
  id: Long,
  name: String,
  elements: Seq[ApiForm.Element],
  showInAggregation: Boolean,
  machineName: String
) extends Response

object ApiForm {

  /**
    * Converts form to ApiForm.
    *
    * @param form form
    */
  def apply(form: Form): ApiForm = ApiForm(
    form.id,
    form.name,
    form.elements.map(Element(_)),
    form.showInAggregation,
    form.machineName
  )

  /**
    * Converts short form to ApiForm.
    *
    * @param form short form
    */
  def apply(form: FormShort): ApiForm = ApiForm(
    form.id,
    form.name,
    Nil,
    form.showInAggregation,
    form.machineName
  )

  implicit val elementValueWrites = Json.writes[ElementValue]
  implicit val elementWrites = Json.writes[Element]
  implicit val formWrites = Json.writes[ApiForm]

  /**
    * Form element api model.
    */
  case class Element(
    id: Long,
    kind: ElementKind,
    caption: String,
    required: Boolean,
    values: Option[Seq[ElementValue]]
  ) extends Response

  object Element {

    /**
      * Converts form element to api form element.
      */
    def apply(element: Form.Element): Element = {
      val values = element.values.map { value =>
        ElementValue(
          value.id,
          value.caption
        )
      }
      Element(
        element.id,
        ElementKind(element.kind),
        element.caption,
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
      "select" -> Select,
      "likedislike" -> LikeDislike
    )
  }

  /**
    * Form element value api model.
    */
  case class ElementValue(
    id: Long,
    caption: String
  ) extends Response {

    def toModel = Form.ElementValue(
      id,
      caption
    )
  }
}
