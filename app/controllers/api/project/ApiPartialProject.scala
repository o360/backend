package controllers.api.project

import controllers.api.Response
import models.project.Project
import play.api.libs.json.Json

/**
  * Project partial API model.
  */
case class ApiPartialProject(
  name: String,
  description: Option[String],
  groupAuditor: Long,
  relations: Seq[ApiProject.Relation]
) extends Response {

  def toModel(id: Long = 0) = Project(
    id,
    name,
    description,
    groupAuditor,
    relations.map(_.toModel)
  )
}

object ApiPartialProject {

  implicit val projectFormat = Json.format[ApiPartialProject]

  def apply(project: Project): ApiPartialProject = ApiPartialProject(
    project.name,
    project.description,
    project.groupAuditor,
    project.relations.map(ApiProject.Relation(_))
  )
}
