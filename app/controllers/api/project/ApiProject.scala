package controllers.api.project

import controllers.api.Response
import models.project.Project
import play.api.libs.json.Json

/**
  * Project API model.
  */
case class ApiProject(
  id: Option[Long],
  name: String,
  description: Option[String],
  groupAuditor: Long,
  relations: Seq[ApiProject.Relation]
) extends Response {

  def toModel(newId: Option[Long] = None) = Project(
    newId.getOrElse(id.getOrElse(0)),
    name,
    description,
    groupAuditor,
    relations.map(_.toModel)
  )
}

object ApiProject {

  /**
    * Relation API model.
    */
  case class Relation(
    groupFrom: Long,
    groupTo: Long,
    form: Long
  ) {

    def toModel = Project.Relation(
      groupFrom,
      groupTo,
      form
    )
  }

  object Relation {
    def apply(r: Project.Relation): Relation = Relation(
      r.groupFrom,
      r.groupTo,
      r.form
    )
  }

  implicit val relationFormat = Json.format[Relation]
  implicit val projectFormat = Json.format[ApiProject]

  def apply(project: Project): ApiProject = ApiProject(
    Some(project.id),
    project.name,
    project.description,
    project.groupAuditor,
    project.relations.map(Relation(_))
  )
}
