package services

import javax.inject.{Inject, Singleton}

import models.dao.{EventDao, TemplateDao}
import models.notification.Notification
import models.template.Template
import models.user.User
import utils.errors.{ExceptionHandler, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

/**
  * Template service.
  */
@Singleton
class TemplateService @Inject()(
  protected val templateDao: TemplateDao,
  protected val eventDao: EventDao
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
    for {
      _ <- getById(id)
      _ <- templateDao.delete(id).lift(ExceptionHandler.sql)
    } yield ()
  }
}
