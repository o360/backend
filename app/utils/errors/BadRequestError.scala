package utils.errors

/**
  * Bad request error.
  */
abstract class BadRequestError(
  code: String,
  message: String,
  inner: Option[Seq[ApplicationError]] = None
) extends ApplicationError(code, message, inner = inner)


object BadRequestError {

  case object Pagination extends BadRequestError("PAGINATION", "Can't parse pagination. Correct usage: ?page=1&size=20")

  object Sorting {
    case object General extends BadRequestError("SORTING", "Can't parse sorting. Correct usage: ?sort=field,-field2,...")

    case class UnsupportedField(unsupported: String, available: String)
      extends BadRequestError("SORTING", s"Unsupported fields: [$unsupported]. Available fields for sorting: [$available]")
  }

  object Event {
    case object StartAfterEnd
      extends BadRequestError("BAD-REQUEST-EVENT-1", "Start date is after end date")

    case object NotUniqueNotifications
      extends BadRequestError("BAD-REQUEST-EVENT-2", "Duplicate notifications")

    case object WrongDates
      extends BadRequestError("BAD-REQUEST-EVENT-3", "Can't use past dates in event")
  }

  object Relation {
    case object ProjectIdIsImmutable
      extends BadRequestError("BAD-REQUEST-RELATION-1", "Can't change relation project ID")

    case object DuplicateRelation
      extends BadRequestError("BAD-REQUEST-RELATION-2", "Duplicate relation")

    case class GroupToMissed(relation: String)
      extends BadRequestError("BAD-REQUEST-RELATION-3", s"GroupTo missed in relation [$relation]")
  }

  object Assessment {
    case class InvalidForm(message: String) extends BadRequestError("BAD-REQUEST-ASSESSMENT-1", message)

    case object RequiredAnswersMissed extends BadRequestError("BAD-REQUEST-ASSESSMENT-2", "Required answers missed")

    case object SelfVoting extends BadRequestError("BAD-REQUEST-ASSESSMENT-3", "Self voting forbidden")

    case class Composite(errors: Seq[ApplicationError])
      extends BadRequestError("BAD-REQUEST-ASSESSMENT-4", "See inner errors", Some(errors))

    case class WithUserFormInfo(error: ApplicationError, userId: Option[Long], formId: Long)
      extends BadRequestError(error.getCode, error.getMessage, error.getInnerErrors)
  }
}
