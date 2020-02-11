package models.dao

import javax.inject.{Inject, Singleton}

import models.{EntityKind, ListWithTotal}
import models.competence.CompetenceGroup
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import utils.listmeta.ListMeta

import scala.concurrent.{ExecutionContext, Future}

/**
  * Component for "competence_group"' table.
  */
trait CompetenceGroupComponent extends EntityKindColumnMapper {
  _: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  class CompetenceGroupTable(tag: Tag) extends Table[CompetenceGroup](tag, "competence_group") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def description = column[Option[String]]("description")
    def kind = column[EntityKind]("kind")
    def machineName = column[String]("machine_name")

    def * = (id, name, description, kind, machineName) <> ((CompetenceGroup.apply _).tupled, CompetenceGroup.unapply)
  }

  val CompetenceGroups = TableQuery[CompetenceGroupTable]
}

/**
  * Competence group DAO.
  */
@Singleton
class CompetenceGroupDao @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider,
  implicit val ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
  with CompetenceGroupComponent
  with DaoHelper {

  import profile.api._

  def create(c: CompetenceGroup): Future[CompetenceGroup] =
    db.run(CompetenceGroups.returning(CompetenceGroups.map(_.id)) += c)
      .map(id => c.copy(id = id))

  def getById(id: Long): Future[Option[CompetenceGroup]] = {
    db.run(CompetenceGroups.filter(_.id === id).result.headOption)
  }

  def getList(
    optKind: Option[EntityKind] = None,
    optIds: Option[Seq[Long]] = None
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[CompetenceGroup]] = {
    val query = CompetenceGroups.applyFilter { cg =>
      Seq(
        optKind.map(cg.kind === _),
        optIds.map(cg.id.inSet(_))
      )
    }

    runListQuery(query) { cg =>
      {
        case "id"          => cg.id
        case "name"        => cg.name
        case "description" => cg.description
      }
    }
  }

  def update(c: CompetenceGroup): Future[CompetenceGroup] =
    db.run(CompetenceGroups.filter(_.id === c.id).update(c))
      .map(_ => c)

  def delete(id: Long): Future[Unit] =
    db.run(CompetenceGroups.filter(_.id === id).delete)
      .map(_ => ())
}
