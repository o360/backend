package controllers.api.project

import controllers.api.{ApiNamedEntity, Response}
import models.project.Project
import models.user.User
import play.api.libs.json.Json

/**
  * Project API model.
  */
case class ApiProject(
  id: Long,
  name: String,
  description: Option[String],
  groupAuditor: Option[ApiNamedEntity],
  templates: Option[Seq[ApiTemplateBinding]]
) extends Response

object ApiProject {

  implicit val projectWrites = Json.writes[ApiProject]

  def apply(project: Project)(implicit account: User): ApiProject = account.role match {
    case User.Role.Admin =>
      ApiProject(
        project.id,
        project.name,
        project.description,
        Some(ApiNamedEntity(project.groupAuditor)),
        Some(project.templates.map(ApiTemplateBinding(_)))
      )
    case User.Role.User =>
      ApiProject(
        project.id,
        project.name,
        project.description,
        None,
        None
      )
  }
}
