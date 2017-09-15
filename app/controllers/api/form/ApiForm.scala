package controllers.api.form

import controllers.api.{EnumFormat, EnumFormatHelper, Response}
import models.form.{Form, FormShort}
import models.form.element._
import play.api.libs.json._
import io.scalaland.chimney.dsl._

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
  def apply(form: Form): ApiForm =
    form
      .into[ApiForm]
      .withFieldComputed(_.elements, _.elements.map(Element(_)))
      .transform

  /**
    * Converts short form to ApiForm.
    *
    * @param form short form
    */
  def apply(form: FormShort): ApiForm =
    form
      .into[ApiForm]
      .withFieldConst(_.elements, Seq.empty[ApiForm.Element])
      .transform

  implicit val elementValueWrites = Json.writes[ElementValue]
  implicit val elementWrites = Json.writes[Element]
  implicit val formWrites = Json.writes[ApiForm]

  /**
    * Form element api model.
    */
  case class Element(
    id: Long,
    kind: ApiElementKind,
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
        ApiElementKind(element.kind),
        element.caption,
        element.required,
        if (values.isEmpty) None else Some(values)
      )
    }
  }

  /**
    * Element kind api model.
    */
  case class ApiElementKind(value: ElementKind) extends EnumFormat[ElementKind]
  object ApiElementKind extends EnumFormatHelper[ElementKind, ApiElementKind]("element kind") {

    override protected def mapping: Map[String, ElementKind] = Map(
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
