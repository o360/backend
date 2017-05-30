package controllers.api.group

import models.group.Group
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
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
    name,
    hasChildren = false
  )
}

object ApiPartialGroup {
  implicit val reads: Reads[ApiPartialGroup] = (
    (__ \ "parentId").readNullable[Long] and
      (__ \ "name").read[String](maxLength[String](1024))
    ) (ApiPartialGroup(_, _))
}
