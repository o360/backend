package controllers.api.project

import controllers.api.{ApiNamedEntity, Response}
import models.project.Project
import play.api.libs.json.Json

/**
  * Project API model.
  */
case class ApiProject(
  id: Long,
  name: String,
  description: Option[String],
  groupAuditor: ApiNamedEntity,
  templates: Seq[ApiTemplateBinding]
) extends Response

object ApiProject {

  implicit val projectWrites = Json.writes[ApiProject]

  def apply(project: Project): ApiProject = ApiProject(
    project.id,
    project.name,
    project.description,
    ApiNamedEntity(project.groupAuditor),
    project.templates.map(ApiTemplateBinding(_))
  )
}
