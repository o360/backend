package services

import javax.inject.{Inject, Singleton}

import models.NamedEntity
import models.dao.{EventDao, ProjectDao, ProjectRelationDao, TemplateDao}
import models.notification.Notification
import models.project.{Project, Relation}
import models.template.Template
import models.user.User
import utils.errors.{ConflictError, ExceptionHandler, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
  * Template service.
  */
@Singleton
class TemplateService @Inject()(
  protected val templateDao: TemplateDao,
  protected val eventDao: EventDao,
  protected val projectDao: ProjectDao,
  protected val relationDao: ProjectRelationDao
) extends ServiceResults[Template] {

  /**
    * Returns template by ID
    */
  def getById(id: Long)(implicit account: User): SingleResult = {
    templateDao.findById(id)
      .liftRight {
        NotFoundError.Template(id)
      }
  }

  /**
    * Returns templates list.
    */
  def getList(
    kind: Option[Notification.Kind],
    recipient: Option[Notification.Recipient]
  )(implicit account: User, meta: ListMeta): ListResult =
    templateDao.getList(optId = None, optKind = kind, optRecipient = recipient).lift

  /**
    * Creates new template.
    *
    * @param template template model
    */
  def create(template: Template)(implicit account: User): SingleResult = {
    templateDao.create(template).lift
  }

  /**
    * Updates template.
    *
    * @param draft template draft
    */
  def update(draft: Template)(implicit account: User): SingleResult = {
    for {
      _ <- getById(draft.id)

      updated <- templateDao.update(draft).lift
    } yield updated
  }

  /**
    * Removes template.
    *
    * @param id template ID
    */
  def delete(id: Long)(implicit account: User): UnitResult = {

    def getConflictedEntities: Future[Option[Map[String, Seq[NamedEntity]]]] = {
      for {
        projects <- projectDao.getList(optEmailTemplateId = Some(id))
        relations <- relationDao.getList(optEmailTemplateId = Some(id))
      } yield {
        ConflictError.getConflictedEntitiesMap(
          Project.namePlural -> (projects.data.map(_.toNamedEntity) ++ relations.data.map(_.project)).distinct
        )
      }
    }

    for {
      _ <- getById(id)

      conflictedEntities <- getConflictedEntities.lift
    _ <- ensure(conflictedEntities.isEmpty) {
      ConflictError.General(Some(Template.nameSingular), conflictedEntities)
    }

      _ <- templateDao.delete(id).lift
    } yield ()
  }
}
