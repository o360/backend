package utils.listmeta.sorting

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


