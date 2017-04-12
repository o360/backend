package controllers.api.group

import models.group.Group
import play.api.libs.json.Json

/**
  * Request for group creating and updating.
  */
case class ApiPartialGroup(
  parentId: Option[Long],
  name: String
) {
  def toModel(id: Long) = Group(
    id,
    parentId,
    name
  )
}

object ApiPartialGroup {
  implicit val reads = Json.reads[ApiPartialGroup]
}
