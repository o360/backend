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

package services

import javax.inject.{Inject, Singleton}

import models.{EntityKind, NamedEntity}
import models.competence.{Competence, CompetenceGroup}
import models.dao.{CompetenceDao, CompetenceGroupDao}
import utils.errors.{ConflictError, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.{ExecutionContext, Future}

/**
  * Competence group service.
  */
@Singleton
class CompetenceGroupService @Inject() (
  protected val competenceGroupDao: CompetenceGroupDao,
  protected val competenceDao: CompetenceDao,
  implicit val ec: ExecutionContext
) extends ServiceResults[CompetenceGroup] {

  def create(cg: CompetenceGroup): SingleResult = competenceGroupDao.create(cg).lift

  def getById(id: Long): SingleResult = competenceGroupDao.getById(id).liftRight {
    NotFoundError.CompetenceGroup(id)
  }

  def getList(implicit meta: ListMeta): ListResult =
    competenceGroupDao.getList(optKind = Some(EntityKind.Template)).lift

  def update(cg: CompetenceGroup): SingleResult = {
    for {
      existed <- getById(cg.id)
      _ <- ensure(existed.kind == cg.kind) {
        ConflictError.Competence.ChangeKind
      }
      updated <- competenceGroupDao.update(cg).lift
    } yield updated
  }

  def delete(id: Long): UnitResult = {
    def getConflictedEntities: Future[Option[Map[String, Seq[NamedEntity]]]] = {
      for {
        competence <- competenceDao.getList(optGroupId = Some(id))
      } yield {
        ConflictError.getConflictedEntitiesMap(
          Competence.namePlural -> competence.data.map(_.toNamedEntity)
        )
      }
    }

    for {
      existed <- getById(id)
      _ <- ensure(existed.kind == EntityKind.Template) {
        ConflictError.Competence.DeleteFreezed
      }
      conflictedEntities <- getConflictedEntities.lift
      _ <- ensure(conflictedEntities.isEmpty) {
        ConflictError.General(Some(CompetenceGroup.nameSingular), conflictedEntities)
      }
      _ <- competenceGroupDao.delete(id).lift
    } yield ()
  }
}
