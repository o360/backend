package controllers.api.project

import controllers.api.Response
import models.project.ActiveProject
import play.api.libs.json.Json

/**
  * Active project API model.
  */
case class ApiActiveProject(
  id: Long,
  name: String,
  description: Option[String],
  formsOnSamePage: Boolean,
  canRevote: Boolean,
  isAnonymous: Boolean
) extends Response

object ApiActiveProject {

  implicit val writes = Json.writes[ApiActiveProject]

  def apply(project: ActiveProject): ApiActiveProject = ApiActiveProject(
    project.id,
    project.name,
    project.description,
    project.formsOnSamePage,
    project.canRevote,
    project.isAnonymous
  )
}
