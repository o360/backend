package controllers.api

import models.NamedEntity
import play.api.libs.json.Json

/**
  * API model for named entity.
  */
case class ApiNamedEntity(
  id: Long,
  name: String
)

object ApiNamedEntity {

  def apply(ne: NamedEntity): ApiNamedEntity = ApiNamedEntity(ne.id, ne.name.getOrElse(""))

  implicit val namedEntityFormat = Json.format[ApiNamedEntity]
}
