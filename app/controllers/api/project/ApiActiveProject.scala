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

package controllers.api.project

import controllers.api.Response
import models.project.ActiveProject
import play.api.libs.json.Json
import io.scalaland.chimney.dsl._

/**
  * Active project API model.
  */
case class ApiActiveProject(
  id: Long,
  name: String,
  description: Option[String],
  formsOnSamePage: Boolean,
  canRevote: Boolean,
  isAnonymous: Boolean,
  userInfo: Option[ApiActiveProject.ApiUserInfo]
) extends Response

object ApiActiveProject {

  implicit val userInfoWrites = Json.writes[ApiUserInfo]
  implicit val writes = Json.writes[ApiActiveProject]

  def apply(project: ActiveProject): ApiActiveProject =
    project
      .into[ApiActiveProject]
      .withFieldComputed(_.userInfo, _.userInfo.map(x => ApiUserInfo(x.isAuditor)))
      .transform

  case class ApiUserInfo(isAuditor: Boolean)
}
