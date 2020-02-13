/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    case object General
      extends BadRequestError("SORTING", "Can't parse sorting. Correct usage: ?sort=field,-field2,...")

    case class UnsupportedField(unsupported: String, available: String)
      extends BadRequestError(
        "SORTING",
        s"Unsupported fields: [$unsupported]. Available fields for sorting: [$available]"
      )
  }

  object Event {
    case object StartAfterEnd extends BadRequestError("BAD-REQUEST-EVENT-1", "Start date is after end date")

    case object NotUniqueNotifications extends BadRequestError("BAD-REQUEST-EVENT-2", "Duplicate notifications")

    case object WrongDates extends BadRequestError("BAD-REQUEST-EVENT-3", "Can't use past dates in event")
  }

  object Relation {
    case object ProjectIdIsImmutable
      extends BadRequestError("BAD-REQUEST-RELATION-1", "Can't change relation project ID")

    case object DuplicateRelation extends BadRequestError("BAD-REQUEST-RELATION-2", "Duplicate relation")

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

  object File {
    case class Extension(actual: String, expected: Set[String])
      extends BadRequestError(
        "BAD-REQUEST-FILE-1",
        s"Invalid file extension [$actual]. Available extensions: [${expected.mkString(", ")}]"
      )

    case object Unexpected extends BadRequestError("BAD-REQUEST-FILE-2", "Unexpected problems with file upload")

    case object Request extends BadRequestError("BAD-REQUEST-FILE-3", "Invalid request")
  }
}
