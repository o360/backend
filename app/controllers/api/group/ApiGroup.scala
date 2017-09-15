package controllers.api.group

import controllers.api.Response
import models.group.Group
import play.api.libs.json.Json
import io.scalaland.chimney.dsl._

/**
  * Response for group model.
  */
case class ApiGroup(
  id: Long,
  parentId: Option[Long],
  name: String,
  hasChildren: Boolean,
  level: Int
) extends Response

object ApiGroup {
  implicit val writes = Json.writes[ApiGroup]

  def apply(group: Group): ApiGroup = group.transformInto[ApiGroup]
}
