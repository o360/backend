package controllers.api.form

import controllers.api.{EnumFormat, EnumFormatHelper, Response}
import models.form.{Form, FormShort}
import play.api.libs.json.Json

/**
  * Api partial form model.
  *
  * @param name     name
  * @param elements form elements
  */
case class ApiPartialForm(
  name: String,
  elements: Option[Seq[ApiForm.Element]]
) extends Response {

  def toModel(id: Long = 0) = Form(
    id,
    name,
    elements.getOrElse(Nil).map(_.toModel)
  )
}

object ApiPartialForm {

  /**
    * Converts form to ApiPartialForm.
    *
    * @param form form
    */
  def apply(form: Form): ApiPartialForm = ApiPartialForm(
    form.name,
    Some(form.elements.map(ApiForm.Element(_)))
  )

  implicit val formFormat = Json.format[ApiPartialForm]
}
