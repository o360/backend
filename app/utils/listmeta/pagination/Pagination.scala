package utils.listmeta.pagination

/**
  * List pagination.
  */
sealed trait Pagination

object Pagination {

  /**
    * Pagination with pages.
    *
    * @param size   page size
    * @param number page number
    */
  case class WithPages(size: Int, number: Int) extends Pagination {

    /**
      * Offset.
      */
    def offset: Int = size * (number - 1)
  }

  /**
    * Unlimited pagination.
    */
  case object WithoutPages extends Pagination

}
