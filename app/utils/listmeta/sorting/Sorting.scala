package utils.listmeta.sorting

import utils.errors.{ApplicationError, BadRequestError}

/**
  * Sorting model.
  *
  * @param fields sort by fields.
  */
case class Sorting(fields: Seq[SortField])

object Sorting {

  private val sortQueryParamName = "sort"
  private val sortPattern = """-?\w+(,-?\w+)*"""

  /**
    * Creates sorting by query string params.
    *
    * @param queryString query string map
    * @param sorting     available sorting fields
    * @return either error or sorting
    */
  def create(queryString: Map[String, String])(implicit sorting: AvailableSortingFields): Either[ApplicationError, Sorting] = {
    queryString.get(sortQueryParamName) match {
      case Some(sort) if sort.matches(sortPattern) =>
        val fields = sort.split(",").map { sortPart =>
          if (sortPart.startsWith("-"))
            SortField(Symbol(sortPart.drop(1)), Direction.Desc)
          else
            SortField(Symbol(sortPart), Direction.Asc)
        }
        val unsupportedFields = fields.map(_.name).filter(!sorting.fields.contains(_))
        if (unsupportedFields.isEmpty)
          Right(this (fields.distinct))
        else
          Left(BadRequestError.Sorting.UnsupportedField(unsupportedFields.mkString(", "), sorting.fields.mkString(", ")))
      case Some(_) =>
        Left(BadRequestError.Sorting.General)
      case None =>
        Right(Sorting(Nil))
    }
  }
}
