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
import play.api.mvc.WrappedRequest
import silhouette.DefaultEnv
import utils.listmeta.ListMeta

/**
  * Secured list request.
  *
  * @param meta  list meta info
  * @param inner inner request
  */
case class ListRequest[B](
  meta: ListMeta,
  inner: SecuredRequest[DefaultEnv, B]
) extends WrappedRequest[B](inner)
  with BaseListRequest
