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
  * Authentication error.
  */
abstract class AuthenticationError(
  code: String,
  message: String
) extends ApplicationError(code, message)

object AuthenticationError {
  case class ProviderNotSupported(providerName: String)
    extends AuthenticationError("PROVIDER_NOT_SUPPORTED", s"Social provider $providerName not supported")

  case object General extends AuthenticationError("GENERAL_AUTHENTICATION", "Not authenticated")
}
