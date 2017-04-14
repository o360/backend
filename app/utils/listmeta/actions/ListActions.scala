package utils.listmeta.actions

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import play.api.mvc._
import silhouette.DefaultEnv
import utils.errors.{ApplicationError, ErrorHelper}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta
import utils.listmeta.pagination.PaginationRequestParser
import utils.listmeta.sorting.{Sorting, SortingRequestParser}

import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * Actions for list requests.
  */
trait ListActions {
  type DefaultSecuredRequest[A] = SecuredRequest[DefaultEnv, A]

  /**
    * Extends secured action with list meta info.
    */
  def ListAction(implicit sortingFields: Sorting.AvailableFields = Sorting.AvailableFields.default) =
    new ActionRefiner[DefaultSecuredRequest, ListRequest] {
      override protected def refine[A](request: DefaultSecuredRequest[A]): Future[Either[Result, ListRequest[A]]] = {
        getListMeta(request.queryString) match {
          case Left(error) => Left(ErrorHelper.getResult(error)).toFuture
          case Right(meta) => Right(ListRequest(meta, request)).toFuture
        }
      }
    }

  /**
    * Extends default action with list meta info.
    */
  def UnsecuredListAction(implicit sortingFields: Sorting.AvailableFields = Sorting.AvailableFields.default) =
    new ActionRefiner[Request, UnsecuredListRequest] {
      override protected def refine[A](request: Request[A]): Future[Either[Result, UnsecuredListRequest[A]]] = {
        getListMeta(request.queryString) match {
          case Left(error) => Left(ErrorHelper.getResult(error)).toFuture
          case Right(meta) => Right(UnsecuredListRequest(meta, request)).toFuture
        }
      }
    }

  /**
    * Returns list meta.
    *
    * @param playQueryString query string
    * @param sortingFields   fields available for sorting
    * @return either list meta or error
    */
  private def getListMeta(playQueryString: Map[String, Seq[String]])
    (implicit sortingFields: Sorting.AvailableFields): Either[ApplicationError, ListMeta] = {
    val queryString = playQueryString.map { case (k, v) => k -> v.mkString }

    for {
      pagination <- PaginationRequestParser.parse(queryString).right
      sorting <- SortingRequestParser.parse(queryString).right
    } yield ListMeta(pagination, sorting)
  }

  /**
    * Converts list request to list meta.
    */
  implicit def request2meta(implicit request: BaseListRequest): ListMeta = request.meta
}
