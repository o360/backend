package utils.listmeta.actions

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import play.api.mvc._
import silhouette.DefaultEnv
import utils.errors.{ApplicationError, ErrorHelper}
import utils.listmeta.ListMeta
import utils.listmeta.pagination.Pagination
import utils.listmeta.sorting.{AvailableSortingFields, Sorting}

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

/**
  * Actions for list requests.
  */
trait ListActions {
  type DefaultSecuredRequest[A] = SecuredRequest[DefaultEnv, A]

  /**
    * Extends secured action with list meta info.
    */
  def ListAction(implicit sortingFields: AvailableSortingFields = AvailableSortingFields.default) =
    new ActionRefiner[DefaultSecuredRequest, ListRequest] {
      override protected def refine[A](request: DefaultSecuredRequest[A]): Future[Either[Result, ListRequest[A]]] = async {
        getListMeta(request.queryString) match {
          case Left(error) => Left(ErrorHelper.getResult(error))
          case Right(meta) => Right(ListRequest(meta, request))
        }
      }
    }

  /**
    * Extends default action with list meta info.
    */
  def UnsecuredListAction(implicit sortingFields: AvailableSortingFields = AvailableSortingFields.default) =
    new ActionRefiner[Request, UnsecuredListRequest] {
      override protected def refine[A](request: Request[A]): Future[Either[Result, UnsecuredListRequest[A]]] = async {
        getListMeta(request.queryString) match {
          case Left(error) => Left(ErrorHelper.getResult(error))
          case Right(meta) => Right(UnsecuredListRequest(meta, request))
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
    (implicit sortingFields: AvailableSortingFields): Either[ApplicationError, ListMeta] = {
    val queryString = playQueryString.map { case (k, v) => k -> v.mkString }

    for {
      pagination <- Pagination.create(queryString).right
      sorting <- Sorting.create(queryString).right
    } yield ListMeta(pagination, sorting)
  }

  /**
    * Converts list request to list meta.
    */
  implicit def request2meta(implicit request: BaseListRequest): ListMeta = request.meta
}
