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
}