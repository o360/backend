package controllers.api.project

import models.project.Relation
import play.api.libs.json.Json

/**
  * Relation partial API model.
  */
case class ApiPartialRelation(
  projectId: Long,
  groupFromId: Long,
  groupToId: Option[Long],
  formId: Long,
  kind: ApiRelation.Kind
) {

  def toModel(id: Long = 0) = Relation(
    id,
    projectId,
    groupFromId,
    groupToId,
    formId,
    kind.value
  )
}

object ApiPartialRelation {
  implicit val relationReads = Json.reads[ApiPartialRelation]
}
