package services

import javax.inject.{Inject, Singleton}

import models.NamedEntity
import models.dao.{EventDao, ProjectDao, ProjectRelationDao, TemplateDao}
import models.notification._
import models.project.Project
import models.template.Template
import utils.errors.{ConflictError, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.{ExecutionContext, Future}

/**
  * Template service.
  */
@Singleton
class TemplateService @Inject() (
  protected val templateDao: TemplateDao,
  protected val eventDao: EventDao,
  protected val projectDao: ProjectDao,
  protected val relationDao: ProjectRelationDao,
  implicit val ec: ExecutionContext
) extends ServiceResults[Template] {

  /**
    * Returns template by ID
    */
  def getById(id: Long): SingleResult = {
    templateDao
      .findById(id)
      .liftRight {
        NotFoundError.Template(id)
      }
  }

  /**
    * Returns templates list.
    */
  def getList(
    kind: Option[NotificationKind],
    recipient: Option[NotificationRecipient]
  )(implicit meta: ListMeta): ListResult =
    templateDao.getList(optId = None, optKind = kind, optRecipient = recipient).lift

  /**
    * Creates new template.
    *
    * @param template template model
    */
  def create(template: Template): SingleResult = {
    templateDao.create(template).lift
  }

  /**
    * Updates template.
    *
    * @param draft template draft
    */
  def update(draft: Template): SingleResult = {
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
  def delete(id: Long): UnitResult = {

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
