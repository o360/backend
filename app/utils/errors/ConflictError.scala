package utils.errors

/**
  * Conflict error.
  */
abstract class ConflictError(
  code: String,
  message: String,
  logMessage: Option[String] = None
) extends ApplicationError(code, message, logMessage)

object ConflictError {
  case class General(logMessage: String)
    extends ConflictError("CONFLICT-GENERAL", "Integrity violation", Some(logMessage))

  object Group {
    case class ParentId(id: Long)
      extends ConflictError("CONFLICT-GROUP-1", s"Group id:$id can't be parent to itself")

    case class CircularReference(id: Long, parentId: Long)
      extends ConflictError("CONFLICT-GROUP-2", s"Can't set parentId:$parentId in group id:$id. Circular reference")

    case object RelationExists extends ConflictError("CONFLICT-GROUP-5", "Can't delete, relation exists")

  }
  object User {
    case class GroupExists(id: Long)
      extends ConflictError("CONFLICT-USER-1", s"Can't delete user id:$id. Group with this user exists")
  }

  object Form {
    case class ElementValuesMissed(element: String)
      extends ConflictError("CONFLICT-FORM-1", s"Missed values list in [$element]")

    case object ActiveEventExists extends ConflictError("CONFLICT-FORM-3", "Can't update, active events exists")

    case object RelationExists extends ConflictError("CONFLICT-FORM-4", "Can't delete, relation exists")

    case class FormKind(action: String) extends ConflictError("CONFLICT-FORM-5", s"Can't $action.")
  }

  object Project {
    case object ActiveEventExists extends ConflictError("CONFLICT-PROJECT-2", "Can't update, active events exists")

    case object EventExists extends ConflictError("CONFLICT-PROJECT-3", "Can't delete, events exists")
  }

  object Template {
    case object ProjectExists extends ConflictError("CONFLICT-TEMPLATE-1", "Can't delete, project exists")

    case object RelationExists extends ConflictError("CONFLICT-RELATION-2", "Can't delete, relation exists")
  }
}
