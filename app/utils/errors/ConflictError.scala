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

    case class ChildrenExists(id: Long, ids: Seq[Long])
      extends ConflictError("CONFLICT-GROUP-3", s"Can't delete group id:$id. Exists children with ids:[${ids.mkString(", ")}]")

    case class UserExists(id: Long)
      extends ConflictError("CONFLICT-GROUP-4", s"Can't delete group id:$id. There are users in group")
  }
  object User {
    case class GroupExists(id: Long)
      extends ConflictError("CONFLICT-USER-1", s"Can't delete user id:$id. Group with this user exists")
  }

  object Form {
    case class ElementValuesMissed(element: String)
      extends ConflictError("CONFLICT-FORM-1", s"Missed values list in [$element]")

    case class DefaultValueNotInValues(element: String)
      extends ConflictError("CONFLICT-FORM-2", s"Default value not in values list in [$element]")
  }

  object Project {
    case object ActiveEventExists extends ConflictError("CONFLICT-PROJECT-2", "Can't update, active events exists")
  }
}
