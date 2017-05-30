package utils.errors

import java.sql.SQLException

/**
  * Exception handlers.
  */
object ExceptionHandler {

  /**
    * Handle sql exceptions.
    */
  val sql: PartialFunction[Throwable, ApplicationError] = {
    case e: SQLException if e.getSQLState.startsWith("23") => // integrity violation
      val constraintNameToError = Seq(
        "event_project_project_id_fk" -> ConflictError.Project.EventExists,
        "project_email_template_template_id_fk" -> ConflictError.Template.ProjectExists,
        "relation_email_template_template_id_fk" -> ConflictError.Template.RelationExists,
        "relation_group_from_id_fk" -> ConflictError.Group.RelationExists
      )

      val message = e.getMessage

      constraintNameToError
        .find(x => message.contains(s""""${x._1}""""))
        .map(_._2)
        .getOrElse(ConflictError.General(logMessage = Some(message)))
  }
}
