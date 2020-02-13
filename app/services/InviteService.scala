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

import models.dao.{GroupDao, InviteDao, UserDao, UserGroupDao}
import models.invite.Invite
import models.user.User
import utils.errors.{ApplicationError, ConflictError, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta
import utils.{RandomGenerator, TimestampConverter}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Invite service.
  */
@Singleton
class InviteService @Inject() (
  userDao: UserDao,
  groupDao: GroupDao,
  inviteDao: InviteDao,
  userGroupDao: UserGroupDao,
  mailService: MailService,
  templateEngineService: TemplateEngineService,
  implicit val ec: ExecutionContext
) extends ServiceResults[Invite] {

  private def validateInvite(invite: Invite): Future[Option[ApplicationError]] = {
    for {
      users <- userDao.getList(optEmail = Some(invite.email))
      groups <- Future.sequence(invite.groups.map(entity => groupDao.findById(entity.id).map((entity, _))))
    } yield {
      val existedEmail = users.data.headOption.flatMap(_.email)
      val notFoundGroupId = groups.collectFirst { case (entity, None) => entity.id }

      (existedEmail, notFoundGroupId) match {
        case (Some(email), _)   => Some(ConflictError.Invite.UserAlreadyRegistered(email))
        case (_, Some(groupId)) => Some(NotFoundError.Group(groupId))
        case _                  => None
      }
    }
  }

  private def prepareInvite(invite: Invite) = invite.copy(
    code = RandomGenerator.generateInviteCode,
    activationTime = None,
    creationTime = TimestampConverter.now
  )

  def createInvites(invites: Seq[Invite]): UnitResult = {
    def sendEmail(invite: Invite): Unit = {
      val bodyTemplate = templateEngineService.loadStaticTemplate("user_invited.html")
      val subject = "Open360 invite"
      val body = templateEngineService.render(bodyTemplate, Map("code" -> invite.code))
      mailService.send(subject, invite.email, invite.email, body)
    }

    for {
      validationResults <- Future.sequence(invites.map(validateInvite)).lift
      _ <- validationResults.collectFirst { case (Some(error)) => error }.liftLeft

      preparedInvites = invites.map(prepareInvite)
      _ <- Future.sequence(preparedInvites.map(inviteDao.create)).lift
      _ = preparedInvites.foreach(sendEmail)
    } yield ()
  }

  def applyInvite(code: String)(implicit account: User): UnitResult = {
    for {
      invite <- inviteDao.findByCode(code).liftRight {
        NotFoundError.Invite(code)
      }
      _ <- ensure(invite.activationTime.isEmpty) {
        ConflictError.Invite.CodeAlreadyUsed
      }
      _ <- ensure(account.status == User.Status.New) {
        ConflictError.Invite.UserAlreadyApproved
      }
      _ <- userDao.update(account.copy(status = User.Status.Approved)).lift
      _ <- Future.sequence(invite.groups.map(x => userGroupDao.add(x.id, account.id))).lift
      _ <- inviteDao.activate(code, TimestampConverter.now).lift
    } yield ()
  }

  def getList(activated: Option[Boolean] = None)(implicit meta: ListMeta = ListMeta.default): ListResult =
    inviteDao.getList(activated = activated).lift
}
