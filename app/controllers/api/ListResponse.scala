package controllers.api

import play.api.libs.json.{JsArray, JsObject, Json, Writes}
import controllers.api.MetaResponse._
import models.ListWithTotal
import utils.listmeta.ListMeta

/**
  * Generic list response model.
  *
  * @param meta meta info
  * @param data elements
  */
case class ListResponse[A](meta: MetaResponse, data: Seq[A]) extends BaseResponse

object ListResponse {
  implicit def writes[A](implicit inner: Writes[A]) = new Writes[ListResponse[A]] {
    override def writes(o: ListResponse[A]) = JsObject(Seq(
      "data" -> JsArray(o.data.map(Json.toJson(_))),
      "meta" -> Json.toJson(o.meta)
    ))
  }

  /**
    * Creates list response from ListWithTotal model.
    *
    * @param list           list with total
    * @param model2response conversion between model and its response
    * @param meta           list meta from request
    * @return list response
    */
  def apply[A, B](list: ListWithTotal[A])(model2response: A => B)(implicit meta: ListMeta): ListResponse[B] = {
    ListResponse(MetaResponse(list.total, meta), list.data.map(model2response))
  }

  /**
    * Creates list response from either ListWithTotal or error.
    *
    * @param list           either list with total or error
    * @param model2response conversion between model and its response
    * @param meta           list meta from request
    * @return either list response or error
    */
  def apply[A, B, E](
    list: Either[E, ListWithTotal[A]]
  )(model2response: A => B)(implicit meta: ListMeta): Either[E, ListResponse[B]] = {
    list.right.map(ListResponse(_)(model2response))
  }
}
