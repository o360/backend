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

package models.dao

import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

/**
  * Component for table 'form_element_competence'
  */
trait FormElementCompetenceComponent { _: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  case class DbFormElementCompetence(
    elementId: Long,
    competenceId: Long,
    factor: Double
  )

  class FormElementCompetenceTable(tag: Tag) extends Table[DbFormElementCompetence](tag, "form_element_competence") {

    def elementId = column[Long]("element_id")
    def competenceId = column[Long]("competence_id")
    def factor = column[Double]("factor")

    def * =
      (elementId, competenceId, factor) <> ((DbFormElementCompetence.apply _).tupled, DbFormElementCompetence.unapply)
  }

  val FormElementCompetencies = TableQuery[FormElementCompetenceTable]
}
