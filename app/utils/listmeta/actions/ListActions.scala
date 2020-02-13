/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils.listmeta.actions

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import play.api.mvc._
import silhouette.DefaultEnv
import utils.errors.{ApplicationError, ErrorHelper}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta
import utils.listmeta.pagination.PaginationRequestParser
import utils.listmeta.sorting.{Sorting, SortingRequestParser}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Actions for list requests.
  */
trait ListActions {
  type DefaultSecuredRequest[A] = SecuredRequest[DefaultEnv, A]

  def ec: ExecutionContext

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
      override protected def executionContext: ExecutionContext = ec
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
      override protected def executionContext: ExecutionContext = ec
    }

  /**
    * Returns list meta.
    *
    * @param playQueryString query string
    * @param sortingFields   fields available for sorting
    * @return either list meta or error
    */
  private def getListMeta(
    playQueryString: Map[String, Seq[String]]
  )(implicit sortingFields: Sorting.AvailableFields): Either[ApplicationError, ListMeta] = {
    val queryString = playQueryString.map { case (k, v) => k -> v.mkString }

    for {
      pagination <- PaginationRequestParser.parse(queryString)
      sorting <- SortingRequestParser.parse(queryString)
    } yield ListMeta(pagination, sorting)
  }

  /**
    * Converts list request to list meta.
    */
  implicit def request2meta(implicit request: BaseListRequest): ListMeta = request.meta
}
