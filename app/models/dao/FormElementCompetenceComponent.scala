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
