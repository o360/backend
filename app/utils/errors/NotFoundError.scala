package utils.errors

/**
  * Not found error.
  */
abstract class NotFoundError(
  code: String,
  message: String
) extends ApplicationError(code, message)

object NotFoundError {
  case class User(id: Long) extends NotFoundError("NOTFOUND-USER", s"Can't find user with id:$id")

  case class Group(id: Long) extends NotFoundError("NOTFOUND-GROUP", s"Can't find group with id:$id")

  case class Form(id: Long) extends NotFoundError("NOTFOUND-FORM", s"Can't find form with id:$id")

  case class Project(id: Long) extends NotFoundError("NOTFOUND-PROJECT", s"Can't find project with id:$id")

  case class ProjectRelation(id: Long) extends NotFoundError("NOTFOUND-RELATION", s"Can't find relation with id:$id")

  case class Event(id: Long) extends NotFoundError("NOTFOUND-EVENT", s"Can't find event with id:$id")

  case class Template(id: Long) extends NotFoundError("NOTFOUND-TEMPLATE", s"Can't find template with id:$id")
}
