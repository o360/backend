package utils.listmeta.sorting

import utils.errors.{ApplicationError, BadRequestError}

/**
  * Parser for sorting.
  */
object SortingRequestParser {

  private val sortQueryParamName = "sort"
  private val sortPattern = """-?\w+(,-?\w+)*"""

  /**
    * Creates sorting by query string params.
    *
    * @param queryString query string map
    * @param sorting     available sorting fields
    * @return either error or sorting
    */
  def parse(
    queryString: Map[String, String]
  )(implicit sorting: Sorting.AvailableFields): Either[ApplicationError, Sorting] = {
    queryString.get(sortQueryParamName) match {
      case Some(sort) if sort.matches(sortPattern) =>
        val fields = sort.split(",").map { sortPart =>
          if (sortPart.startsWith("-"))
            Sorting.Field(sortPart.drop(1), Sorting.Direction.Desc)
          else
            Sorting.Field(sortPart, Sorting.Direction.Asc)
        }
        val unsupportedFields = fields.map(_.name).filter(!sorting.fields.contains(_))
        if (unsupportedFields.isEmpty)
          Right(Sorting(fields.distinct.toIndexedSeq))
        else
          Left(
            BadRequestError.Sorting.UnsupportedField(unsupportedFields.mkString(", "), sorting.fields.mkString(", "))
          )
      case Some(_) =>
        Left(BadRequestError.Sorting.General)
      case None =>
        Right(Sorting(Nil))
    }
  }

}
