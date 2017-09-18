package controllers.api.form

import controllers.api.Response
import controllers.api.form.ApiForm.ApiElementKind
import models.form.Form
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import utils.RandomGenerator
import io.scalaland.chimney.dsl._
import models.NamedEntity

/**
  * Api partial form model.
  */
case class ApiPartialForm(
  name: String,
  elements: Option[Seq[ApiPartialForm.Element]],
  showInAggregation: Boolean,
  machineName: Option[String]
) {

  def toModel(id: Long = 0) =
    this
      .into[Form]
      .withFieldConst(_.id, id)
      .withFieldComputed(_.elements, _.elements.getOrElse(Seq.empty[ApiPartialForm.Element]).map(_.toModel))
      .withFieldConst(_.kind, Form.Kind.Active: Form.Kind)
      .withFieldComputed(_.machineName, _.machineName.getOrElse(RandomGenerator.generateMachineName))
      .transform
}

object ApiPartialForm {

  implicit val elementCompetenceReads = Json.reads[ElementCompetence]

  implicit val elementValueReads: Reads[ElementValue] =
    (__ \ "caption").read[String](maxLength[String](1024)).map(ElementValue)

  implicit val elementReads: Reads[Element] = (
    (__ \ "kind").read[ApiElementKind] and
      (__ \ "caption").read[String](maxLength[String](1024)) and
      (__ \ "required").read[Boolean] and
      (__ \ "values").readNullable[Seq[ElementValue]] and
      (__ \ "competencies").readNullable[Seq[ElementCompetence]]
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
    kind: ApiElementKind,
    caption: String,
    required: Boolean,
    values: Option[Seq[ElementValue]],
    competencies: Option[Seq[ElementCompetence]]
  ) extends Response {

    def toModel =
      this
        .into[Form.Element]
        .withFieldConst(_.id, 0L)
        .withFieldComputed(_.kind, _.kind.value)
        .withFieldComputed(_.values, _.values.getOrElse(Seq.empty[ApiPartialForm.ElementValue]).map(_.toModel))
        .withFieldComputed(_.competencies, _.competencies.toSeq.flatMap(_.map(_.toModel)))
        .transform
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

  /**
    * Form element competence API model.
    */
  case class ElementCompetence(
    competenceId: Long,
    factor: Double
  ) {
    def toModel = Form.ElementCompetence(
      NamedEntity(competenceId),
      factor
    )
  }
}
