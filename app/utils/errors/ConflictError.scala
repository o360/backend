package utils.errors

/**
  * Conflict error.
  */
abstract class ConflictError(
  code: String,
  message: String
) extends ApplicationError(code, message)

object ConflictError {
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
}
