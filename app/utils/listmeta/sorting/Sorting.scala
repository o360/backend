package utils.listmeta.sorting

/**
  * Sorting model.
  *
  * @param fields sort by fields.
  */
case class Sorting(fields: Seq[Sorting.Field])

object Sorting {

  /**
    * Sort by field.
    *
    * @param name      field name
    * @param direction sorting direction
    */
  case class Field(name: String, direction: Direction)

  /**
    * Sorting direction.
    */
  sealed trait Direction
  object Direction {

    /**
      * Ascending direction.
      */
    case object Asc extends Direction

    /**
      * Descending direction.
      */
    case object Desc extends Direction

  }

  /**
    * List of fields available for sorting.
    */
  case class AvailableFields(fields: Set[String])
  object AvailableFields {

    /**
      * Creates available sorting fields.
      *
      * @param fields fields list
      */
    def apply(fields: String*): AvailableFields = AvailableFields(fields.toSet)

    /**
      * Default sorting fields.
      */
    def default: AvailableFields = AvailableFields()
  }
}
