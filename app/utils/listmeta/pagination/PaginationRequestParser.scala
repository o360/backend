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

package utils.listmeta.pagination

import utils.errors.{ApplicationError, BadRequestError}
import utils.listmeta.pagination.Pagination.{WithPages, WithoutPages}

import scala.util.{Success, Try}

/**
  * Parser for pagination.
  */
object PaginationRequestParser {

  private val sizeQueryParamName = "size"
  private val numberQueryParamName = "number"

  /**
    * Creates pagination by query string params.
    *
    * @param queryString query string map
    * @return either pagination or error
    */
  def parse(queryString: Map[String, String]): Either[ApplicationError, Pagination] = {

    val sizeStringOption = queryString.get(sizeQueryParamName)
    val numberStringOption = queryString.get(numberQueryParamName)

    (sizeStringOption, numberStringOption) match {
      case (Some(sizeStr), Some(numberStr)) =>
        Try((sizeStr.toInt, numberStr.toInt)) match {
          case Success((size, number)) if size >= 0 && number > 0 => Right(WithPages(size, number))
          case _                                                  => Left(BadRequestError.Pagination)
        }

      case (Some(sizeStr), None) =>
        Try(sizeStr.toInt) match {
          case Success(size) if size >= 0 => Right(WithPages(size, 1))
          case _                          => Left(BadRequestError.Pagination)
        }

      case _ => Right(WithoutPages)
    }
  }
}
