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
  * Relation inside project.
  */
case class Relation(
  id: Long,
  project: NamedEntity,
  groupFrom: NamedEntity,
  groupTo: Option[NamedEntity],
  form: NamedEntity,
  kind: Relation.Kind,
  templates: Seq[TemplateBinding],
  hasInProgressEvents: Boolean,
  canSelfVote: Boolean,
  canSkipAnswers: Boolean
) {
  def toNamedEntity = {
    NamedEntity(id, s"${groupFrom.name.getOrElse("")} -> ${groupTo.flatMap(_.name).getOrElse("...")}")
  }
}

object Relation {
  val namePlural = "relations"

  /**
    * Kind of relation.
    */
  sealed trait Kind
  object Kind {

    /**
      * Group2group.
      */
    case object Classic extends Kind

    /**
      * Survey, single group.
      */
    case object Survey extends Kind
  }
}
