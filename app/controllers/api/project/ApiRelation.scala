package controllers.api.project

import controllers.api.{EnumFormat, EnumFormatHelper, Response}
import models.project.Relation
import play.api.libs.json.Json

/**
  * Relation API model.
  */
case class ApiRelation(
  id: Long,
  projectId: Long,
  groupFromId: Long,
  groupToId: Option[Long],
  formId: Long,
  kind: ApiRelation.Kind
) extends Response

object ApiRelation {
  def apply(r: Relation): ApiRelation = ApiRelation(
    r.id,
    r.projectId,
    r.groupFrom,
    r.groupTo,
    r.form,
    Kind(r.kind)
  )

  case class Kind(value: Relation.Kind) extends EnumFormat[Relation.Kind]
  object Kind extends EnumFormatHelper[Relation.Kind, Kind]("relation kind") {

    override protected def mapping: Map[String, Relation.Kind] = Map(
      "classic" -> Relation.Kind.Classic,
      "survey" -> Relation.Kind.Survey
    )
  }

  implicit val relationWrites = Json.format[ApiRelation]
}
