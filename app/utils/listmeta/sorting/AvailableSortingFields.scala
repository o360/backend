package utils.listmeta.sorting

/**
  * List of fields available for sorting.
  */
case class AvailableSortingFields(fields: Set[Symbol])

object AvailableSortingFields {
  /**
    * Creates available sorting fields.
    *
    * @param fields fields list
    */
  def apply(fields: Symbol*): AvailableSortingFields = this (fields.toSet)

  /**
    * Default sorting fields.
    */
  def default: AvailableSortingFields = this ()
}
