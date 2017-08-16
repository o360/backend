package controllers.api

import controllers.api.Response.Error.AdditionalInfo
import models.ListWithTotal
import play.api.libs.json._
import utils.listmeta.ListMeta
import utils.listmeta.pagination.Pagination.{WithPages, WithoutPages}

/**
  * Base trait for api responses.
  */
trait Response

object Response {

  /**
    * Error response model.
    *
    * @param code    error code
    * @param message error message
    */
  case class Error(
    code: String,
    message: String,
    additionalInfo: Option[AdditionalInfo] = None,
    inner: Option[Seq[Error]] = None
  ) extends Response

  object Error {

    /**
      * Error additional info.
      */
    sealed trait AdditionalInfo {

      /**
        * Converts additional info to JsObject.
        */
      def toJson: JsObject
    }

    object AdditionalInfo {

      /**
        * List of conflicted dependencies.
        */
      case class ConflictDependencies(values: Map[String, Seq[ApiNamedEntity]]) extends AdditionalInfo {
        def toJson: JsObject =
          JsObject(
            Seq(
              "conflicts" -> JsObject(
                values.mapValues(x => JsArray(x.map(Json.toJson(_))))
              )))
      }

      case class UserFormInfo(userId: Option[Long], formId: Long) extends AdditionalInfo {
        def toJson: JsObject =
          JsObject(
            Seq(
              "userId" -> Json.toJson(userId),
              "formId" -> Json.toJson(formId)
            ))
      }
    }

    implicit val writes = new Writes[Error] {
      override def writes(o: Error): JsValue = {
        var errorJs = JsObject(
          Seq(
            "code" -> JsString(o.code),
            "message" -> JsString(o.message)
          ))

        o.additionalInfo.map(_.toJson).foreach(errorJs ++= _)

        o.inner.foreach { innerErrors =>
          errorJs ++= JsObject(
            Seq(
              "inner" -> JsArray(innerErrors.map(Json.toJson(_)(this)))
            ))
        }

        errorJs
      }
    }
  }

  /**
    * Generic list response model.
    *
    * @param meta meta info
    * @param data elements
    */
  case class List[A](meta: Meta, data: Seq[A]) extends Response
  object List {
    implicit def writes[A](implicit inner: Writes[A]) = new Writes[List[A]] {
      override def writes(o: List[A]) =
        JsObject(
          Seq(
            "data" -> JsArray(o.data.map(Json.toJson(_))),
            "meta" -> Json.toJson(o.meta)(Meta.writes)
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
    def apply[A, B](list: ListWithTotal[A])(model2response: A => B)(implicit meta: ListMeta): List[B] = {
      List(Meta(list.total, meta), list.data.map(model2response))
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
    )(model2response: A => B)(implicit meta: ListMeta): Either[E, List[B]] = {
      list.right.map(List(_)(model2response))
    }
  }

  /**
    * List meta info response model.
    *
    * @param total  total number of elements
    * @param size   page size
    * @param number page number
    */
  case class Meta(
    total: Int,
    size: Int,
    number: Int
  ) extends Response
  object Meta {
    implicit val writes = Json.writes[Meta]

    /**
      * Creates meta response by total count and list meta.
      */
    def apply(total: Int, listMeta: ListMeta): Meta = {
      val (size, number) = listMeta.pagination match {
        case WithoutPages => (total, 1)
        case WithPages(s, n) => (s, n)
      }
      Meta(
        total,
        size,
        number
      )
    }
  }

}
