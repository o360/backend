package utils.listmeta

import utils.listmeta.pagination.Pagination
import utils.listmeta.sorting.Sorting

/**
  * List request meta information.
  *
  * @param pagination pagination
  * @param sorting    sorting
  */
case class ListMeta(pagination: Pagination, sorting: Sorting)

object ListMeta {

  /**
    * Default list meta. Disabled sorting and pagination.
    */
  def default: ListMeta = this(
    Pagination.WithoutPages,
    Sorting(Nil)
  )
}
