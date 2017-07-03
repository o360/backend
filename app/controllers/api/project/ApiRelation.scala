package controllers.api.project

import controllers.api.{ApiNamedEntity, EnumFormat, EnumFormatHelper, Response}
import models.project.Relation
import play.api.libs.json.Json

/**
  * Relation API model.
  */
case class ApiRelation(
  id: Long,
  project: ApiNamedEntity,
  groupFrom: ApiNamedEntity,
  groupTo: Option[ApiNamedEntity],
  form: ApiNamedEntity,
  kind: ApiRelation.Kind,
  templates: Seq[ApiTemplateBinding],
  hasInProgressEvents: Boolean
) extends Response

object ApiRelation {
  def apply(r: Relation): ApiRelation = ApiRelation(
    r.id,
    ApiNamedEntity(r.project),
    ApiNamedEntity(r.groupFrom),
    r.groupTo.map(ApiNamedEntity(_)),
    ApiNamedEntity(r.form),
    Kind(r.kind),
    r.templates.map(ApiTemplateBinding(_)),
    r.hasInProgressEvents
  )

  case class Kind(value: Relation.Kind) extends EnumFormat[Relation.Kind]
  object Kind extends EnumFormatHelper[Relation.Kind, Kind]("relation kind") {

    override protected def mapping: Map[String, Relation.Kind] = Map(
      "classic" -> Relation.Kind.Classic,
      "survey" -> Relation.Kind.Survey
    )
  }

  implicit val relationWrites = Json.writes[ApiRelation]
}
