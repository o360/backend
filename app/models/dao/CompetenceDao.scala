package models.dao

import javax.inject.{Inject, Singleton}

import models.{EntityKind, ListWithTotal}
import models.competence.Competence
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import utils.listmeta.ListMeta

import scala.concurrent.{ExecutionContext, Future}

/**
  * Component for 'competence' table.
  */
trait CompetenceComponent extends EntityKindColumnMapper { _: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  class CompetenceTable(tag: Tag) extends Table[Competence](tag, "competence") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def groupId = column[Long]("group_id")
    def name = column[String]("name")
    def description = column[Option[String]]("description")
    def kind = column[EntityKind]("kind")

    def * = (id, groupId, name, description, kind) <> ((Competence.apply _).tupled, Competence.unapply)
  }

  val Competencies = TableQuery[CompetenceTable]
}

/**
  * Competence DAO.
  */
@Singleton
class CompetenceDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  implicit val ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
  with CompetenceComponent
  with CompetenceGroupComponent
  with DaoHelper {

  import profile.api._

  def create(c: Competence): Future[Competence] =
    db.run(Competencies.returning(Competencies.map(_.id)) += c)
      .map(id => c.copy(id = id))

  def getById(id: Long): Future[Option[Competence]] = {
    db.run(Competencies.filter(_.id === id).result.headOption)
  }

  def getList(
    optGroupId: Option[Long] = None,
    optKind: Option[EntityKind] = None,
    optIds: Option[Seq[Long]] = None
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[Competence]] = {
    val query = Competencies.applyFilter { c =>
      Seq(
        optGroupId.map(c.groupId === _),
        optKind.map(c.kind === _),
        optIds.map(c.id.inSet(_))
      )
    }

    runListQuery(query) { c =>
      {
        case 'id => c.id
        case 'groupId => c.groupId
        case 'name => c.name
        case 'description => c.description
      }
    }
  }

  def update(c: Competence): Future[Competence] =
    db.run(Competencies.filter(_.id === c.id).update(c))
      .map(_ => c)

  def delete(id: Long): Future[Unit] =
    db.run(Competencies.filter(_.id === id).delete)
      .map(_ => ())

}
