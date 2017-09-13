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
  isAnonymous: Boolean,
  userInfo: Option[ApiActiveProject.ApiUserInfo]
) extends Response

object ApiActiveProject {

  implicit val userInfoWrites = Json.writes[ApiUserInfo]
  implicit val writes = Json.writes[ApiActiveProject]

  def apply(project: ActiveProject): ApiActiveProject = ApiActiveProject(
    project.id,
    project.name,
    project.description,
    project.formsOnSamePage,
    project.canRevote,
    project.isAnonymous,
    project.userInfo.map(x => ApiUserInfo(x.isAuditor))
  )

  case class ApiUserInfo(isAuditor: Boolean)
}
