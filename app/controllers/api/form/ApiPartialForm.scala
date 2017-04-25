package controllers.api.form

import controllers.api.Response
import models.form.Form
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._


/**
  * Api partial form model.
  *
  * @param name     name
  * @param elements form elements
  */
case class ApiPartialForm(
  name: String,
  elements: Option[Seq[ApiForm.Element]]
) {

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

  implicit val formReads: Reads[ApiPartialForm] = (
    (__ \ "name").read[String](maxLength[String](1024)) and
      (__ \ "elements").readNullable[Seq[ApiForm.Element]]
    ) (ApiPartialForm(_, _))
}
