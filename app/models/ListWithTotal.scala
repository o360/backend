package models

/**
  * List result with total count info.
  *
  * @param total total count
  * @param data  list elements
  */
case class ListWithTotal[A](total: Int, data: Seq[A])

object ListWithTotal {
  def apply[A](data: Seq[A]): ListWithTotal[A] = ListWithTotal(data.size, data)
}
