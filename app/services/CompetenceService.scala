package services

import javax.inject.{Inject, Singleton}

import models.EntityKind
import models.competence.Competence
import models.dao.CompetenceDao
import utils.errors.{ConflictError, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.ExecutionContext

/**
  * Comptence service.
  */
@Singleton
class CompetenceService @Inject() (
  protected val competenceDao: CompetenceDao,
  protected val competenceGroupService: CompetenceGroupService,
  implicit val ec: ExecutionContext
) extends ServiceResults[Competence] {

  def create(c: Competence): SingleResult = {
    for {
      _ <- competenceGroupService.getById(c.groupId)
      created <- competenceDao.create(c).lift
    } yield created
  }

  def getById(id: Long): SingleResult = competenceDao.getById(id).liftRight {
    NotFoundError.Competence(id)
  }

  def getList(groupId: Option[Long])(implicit meta: ListMeta): ListResult =
    competenceDao
      .getList(
        optGroupId = groupId,
        optKind = Some(EntityKind.Template)
      )
      .lift

  def update(c: Competence): SingleResult = {
    for {
      existed <- getById(c.id)
      _ <- ensure(existed.kind == c.kind) {
        ConflictError.Competence.ChangeKind
      }
      _ <- competenceGroupService.getById(c.groupId)
      updated <- competenceDao.update(c).lift
    } yield updated
  }

  def delete(id: Long): UnitResult = {
    for {
      existed <- getById(id)
      _ <- ensure(existed.kind == EntityKind.Template) {
        ConflictError.Competence.DeleteFreezed
      }
      _ <- competenceDao.delete(id).lift
    } yield ()
  }
}
