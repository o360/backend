package utils.errors

/**
  * Bad request error.
  */
abstract class BadRequestError(
  code: String,
  message: String
) extends ApplicationError(code, message)


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
  }

  object Relation {
    case object ProjectIdIsImmutable
      extends BadRequestError("BAD-REQUEST-RELATION-1", "Can't change relation project ID")

    case object DuplicateRelation
      extends BadRequestError("BAD-REQUEST-RELATION-2", "Duplicate relation")

    case class GroupToMissed(relation: String)
      extends BadRequestError("BAD-REQUEST-RELATION-3", s"GroupTo missed in relation [$relation]")
  }
}
