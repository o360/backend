package models.dao

import javax.inject.{Inject, Singleton}

import models.ListWithTotal
import models.notification.Notification
import models.template.Template
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import utils.listmeta.ListMeta

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

/**
  * Component for template table.
  */
trait TemplateComponent extends NotificationComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  class TemplateTable(tag: Tag) extends Table[Template](tag, "template") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def name = column[String]("name")
    def subject = column[String]("subject")
    def body = column[String]("body")
    def kind = column[Notification.Kind]("kind")
    def recipient = column[Notification.Recipient]("recipient_kind")

    def * = (id, name, subject, body, kind, recipient) <> ((Template.apply _).tupled, Template.unapply)
  }

  val Templates = TableQuery[TemplateTable]
}

/**
  * Template DAO.
  */
@Singleton
class TemplateDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
  with TemplateComponent
  with DaoHelper {

  import driver.api._

  /**
    * Creates template.
    *
    * @return template model with ID
    */
  def create(model: Template): Future[Template] = db.run {
    Templates.returning(Templates.map(_.id)).into { case (_, id) => model.copy(id = id) } += model
  }

  /**
    * Updates template.
    */
  def update(model: Template): Future[Template] = db.run {
    Templates.filter(_.id === model.id).update(model).map(_ => model)
  }

  /**
    * Returns list of templates filtered by given criteria.
    */
  def getList(
    optId: Option[Long] = None,
    optKind: Option[Notification.Kind] = None,
    optRecipient: Option[Notification.Recipient] = None
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[Template]] = {
    val query = Templates
      .applyFilter { x =>
        Seq(
          optId.map(x.id === _),
          optKind.map(x.kind === _),
          optRecipient.map(x.recipient === _)
        )
      }

    runListQuery(query) { template =>
      {
        case 'id => template.id
        case 'name => template.name
        case 'kind => template.kind
        case 'recipient => template.recipient
      }
    }
  }

  /**
    * Finds template by ID.
    */
  def findById(id: Long): Future[Option[Template]] = {
    getList(optId = Some(id)).map(_.data.headOption)
  }

  /**
    * Removes template.
    */
  def delete(templateId: Long): Future[Int] = db.run {
    Templates.filter(_.id === templateId).delete
  }
}
