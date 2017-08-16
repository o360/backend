package controllers.api.form

import controllers.api.Response
import controllers.api.form.ApiForm.ElementKind
import models.form.Form
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.RandomGenerator

/**
  * Api partial form model.
  */
case class ApiPartialForm(
  name: String,
  elements: Option[Seq[ApiPartialForm.Element]],
  showInAggregation: Boolean,
  machineName: Option[String]
) {

  def toModel(id: Long = 0) = Form(
    id,
    name,
    elements.getOrElse(Nil).map(_.toModel),
    Form.Kind.Active,
    showInAggregation,
    machineName.getOrElse(RandomGenerator.generateMachineName)
  )
}

object ApiPartialForm {

  implicit val elementValueReads: Reads[ElementValue] =
    (__ \ "caption").read[String](maxLength[String](1024)).map(ElementValue)

  implicit val elementReads: Reads[Element] = (
    (__ \ "kind").read[ElementKind] and
      (__ \ "caption").read[String](maxLength[String](1024)) and
      (__ \ "required").read[Boolean] and
      (__ \ "values").readNullable[Seq[ElementValue]]
  )(Element)

  implicit val formReads: Reads[ApiPartialForm] = (
    (__ \ "name").read[String](maxLength[String](1024)) and
      (__ \ "elements").readNullable[Seq[ApiPartialForm.Element]] and
      (__ \ "showInAggregation").read[Boolean] and
      (__ \ "machineName").readNullable[String]
  )(ApiPartialForm(_, _, _, _))

  /**
    * Form element api model.
    */
  case class Element(
    kind: ElementKind,
    caption: String,
    required: Boolean,
    values: Option[Seq[ElementValue]]
  ) extends Response {

    def toModel = Form.Element(
      0,
      kind.value,
      caption,
      required,
      values.getOrElse(Nil).map(_.toModel)
    )
  }

  /**
    * Form element value api model.
    */
  case class ElementValue(
    caption: String
  ) extends Response {

    def toModel = Form.ElementValue(
      0,
      caption
    )
  }
}
