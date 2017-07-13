package controllers.api.group

import play.api.libs.json.Json

/**
  * Request for bulk user-group.
  */
case class ApiUserGroup(
  groupId: Long,
  userId: Long
)

object ApiUserGroup {
  implicit val reads = Json.reads[ApiUserGroup]
}
