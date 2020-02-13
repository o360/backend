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

package utils.errors

/**
  * Base application error.
  *
  * @param code       error code
  * @param message    error message
  * @param logMessage log message
  * @param inner      inner errors
  */
abstract class ApplicationError(
  code: String,
  message: String,
  logMessage: Option[String] = None,
  inner: Option[Seq[ApplicationError]] = None
) {

  /**
    * Returns error code.
    */
  def getCode = code

  /**
    * Returns error message.
    */
  def getMessage = message

  /**
    * Returns log message.
    */
  def getLogMessage = logMessage

  /**
    * Returns inner errors.
    */
  def getInnerErrors = inner
}
