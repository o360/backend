package controllers.api

import play.api.libs.json.Json
import utils.listmeta.ListMeta
import utils.listmeta.pagination.Pagination.{WithPages, WithoutPages}

/**
  * List meta info response model.
  *
  * @param total  total number of elements
  * @param size   page size
  * @param number page number
  */
case class MetaResponse(
  total: Int,
  size: Int,
  number: Int
) extends BaseResponse

object MetaResponse {
  implicit val writes = Json.writes[MetaResponse]

  /**
    * Creates meta response by total count and list meta.
    */
  def apply(total: Int, listMeta: ListMeta): MetaResponse = {
    val (size, number) = listMeta.pagination match {
      case WithoutPages => (total, 1)
      case WithPages(s, n) => (s, n)
    }
    this (
      total,
      size,
      number
    )
  }
}
