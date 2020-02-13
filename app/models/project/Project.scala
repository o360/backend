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

package models.project

import models.NamedEntity

/**
  * Project model.
  *
  * @param id                  DB iD
  * @param name                project name
  * @param description         description
  * @param groupAuditor        group-auditor
  * @param templates           project-wide email templates
  * @param formsOnSamePage     is all forms displayed on same assessment page
  * @param canRevote           if true user can revote
  * @param isAnonymous         is answers in project anonymous by default
  * @param hasInProgressEvents is project has in progress events
  * @param machineName         machine name
  */
case class Project(
  id: Long,
  name: String,
  description: Option[String],
  groupAuditor: NamedEntity,
  templates: Seq[TemplateBinding],
  formsOnSamePage: Boolean,
  canRevote: Boolean,
  isAnonymous: Boolean,
  hasInProgressEvents: Boolean,
  machineName: String
) {
  def toNamedEntity = NamedEntity(id, name)
}

object Project {
  val namePlural = "projects"
  val nameSingular = "project"
}
