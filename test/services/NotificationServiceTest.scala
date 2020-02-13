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

import java.time.LocalDateTime

import models.dao.{EventDao, ProjectDao, ProjectRelationDao, TemplateDao}
import models.event.{Event, EventJob}
import models.notification._
import models.project.{Project, Relation, TemplateBinding}
import models.{ListWithTotal, NamedEntity}
import org.mockito.Mockito._
import testutils.fixture.{EventFixture, ProjectFixture, ProjectRelationFixture, UserFixture}
import testutils.generator.{EventGenerator, ListWithTotalGenerator}

/**
  * Test for notification service.
  */
class NotificationServiceTest
  extends BaseServiceTest
  with EventGenerator
  with ListWithTotalGenerator
  with EventFixture
  with ProjectFixture
  with UserFixture
  with ProjectRelationFixture {

  private case class Fixture(
    eventDao: EventDao,
    projectDao: ProjectDao,
    relationDao: ProjectRelationDao,
    templateDao: TemplateDao,
    userService: UserService,
    mailService: MailService,
    templateEngineService: TemplateEngineService,
    service: NotificationService
  )

  private def getFixture = {
    val eventDao = mock[EventDao]
    val projectDao = mock[ProjectDao]
    val relationDao = mock[ProjectRelationDao]
    val templateDao = mock[TemplateDao]
    val userService = mock[UserService]
    val mailService = mock[MailService]
    val templateEngineService = mock[TemplateEngineService]
    val service = new NotificationService(
      eventDao,
      projectDao,
      relationDao,
      templateDao,
      userService,
      mailService,
      templateEngineService,
      ec
    )
    Fixture(eventDao, projectDao, relationDao, templateDao, userService, mailService, templateEngineService, service)
  }

  private val notificationFixture =
    Event.NotificationTime(LocalDateTime.MIN, Begin, Respondent)
  private val jobFixture = EventJob.SendNotification(0, 1, notificationFixture, EventJob.Status.New)

  "execute" should {
    "do nothing if there is no events" in {
      val fixture = getFixture

      when(
        fixture.eventDao.getList(
          optId = eqTo(Some(jobFixture.eventId)),
          optStatus = *,
          optProjectId = *,
          optFormId = *,
          optGroupFromIds = *,
          optUserId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))

      wait(fixture.service.execute(jobFixture))
      succeed
    }

    "query projects for each event" in {
      val fixture = getFixture
      val event = Events(0)

      when(
        fixture.eventDao.getList(
          optId = eqTo(Some(jobFixture.eventId)),
          optStatus = *,
          optProjectId = *,
          optFormId = *,
          optGroupFromIds = *,
          optUserId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal(1, Seq(event))))

      when(
        fixture.projectDao.getList(
          optId = *,
          optEventId = eqTo(Some(event.id)),
          optGroupFromIds = *,
          optFormId = *,
          optGroupAuditorId = *,
          optEmailTemplateId = *,
          optAnyRelatedGroupId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal[Project](0, Nil)))

      wait(fixture.service.execute(jobFixture))
      succeed
    }

    "send emails to auditor" in {
      val fixture = getFixture
      val notification = notificationFixture.copy(recipient = Auditor)
      val event = Events(0).copy(notifications = Seq(notification))
      val project = Projects(0).copy(
        templates = Seq(
          TemplateBinding(
            NamedEntity(1, "template name"),
            notification.kind,
            notification.recipient
          )
        )
      )
      val template = Templates(0)
      val user = Users(0)
      val job = jobFixture.copy(notification = notification)

      when(
        fixture.eventDao.getList(
          optId = eqTo(Some(job.eventId)),
          optStatus = *,
          optProjectId = *,
          optFormId = *,
          optGroupFromIds = *,
          optUserId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal(1, Seq(event))))

      when(
        fixture.projectDao.getList(
          optId = *,
          optEventId = eqTo(Some(event.id)),
          optGroupFromIds = *,
          optFormId = *,
          optGroupAuditorId = *,
          optEmailTemplateId = *,
          optAnyRelatedGroupId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal(1, Seq(project))))

      when(fixture.templateDao.findById(1)).thenReturn(toFuture(Some(template)))

      when(
        fixture.relationDao.getList(
          optId = *,
          optProjectId = eqTo(Some(project.id)),
          optKind = *,
          optFormId = *,
          optGroupFromId = *,
          optGroupToId = *,
          optEmailTemplateId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal[Relation](0, Nil)))

      when(
        fixture.userService.listByGroupId(
          groupId = eqTo(project.groupAuditor.id),
          includeDeleted = eqTo(false)
        )(*)
      ).thenReturn(toSuccessResult(ListWithTotal(1, Seq(user))))

      val context = Map("example" -> "any")
      val renderedSubject = "subject"
      val renderedBody = "body"

      when(fixture.templateEngineService.getContext(user, Some(event))).thenReturn(context)
      when(fixture.templateEngineService.render(template.subject, context)).thenReturn(renderedSubject)
      when(fixture.templateEngineService.render(template.body, context)).thenReturn(renderedBody)

      wait(fixture.service.execute(job))
      verify(fixture.mailService, times(1)).send(renderedSubject, user, renderedBody)
    }

    "send emails to respondent" in {
      val fixture = getFixture
      val notification = notificationFixture.copy(recipient = Respondent)

      val event = Events(0).copy(notifications = Seq(notification))
      val project = Projects(0).copy(
        templates = Seq(
          TemplateBinding(
            NamedEntity(1, "template name"),
            notification.kind,
            notification.recipient
          )
        )
      )
      val template = Templates(0)
      val user = Users(0)
      val relation = ProjectRelations(0).copy(
        templates = Seq(
          TemplateBinding(
            NamedEntity(1, "template name"),
            notification.kind,
            notification.recipient
          )
        )
      )
      val job = jobFixture.copy(notification = notification)

      when(
        fixture.eventDao.getList(
          optId = eqTo(Some(job.eventId)),
          optStatus = *,
          optProjectId = *,
          optFormId = *,
          optGroupFromIds = *,
          optUserId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal(1, Seq(event))))

      when(
        fixture.projectDao.getList(
          optId = *,
          optEventId = eqTo(Some(event.id)),
          optGroupFromIds = *,
          optFormId = *,
          optGroupAuditorId = *,
          optEmailTemplateId = *,
          optAnyRelatedGroupId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal(1, Seq(project))))

      when(fixture.templateDao.findById(1)).thenReturn(toFuture(Some(template)))

      when(
        fixture.relationDao.getList(
          optId = *,
          optProjectId = eqTo(Some(project.id)),
          optKind = *,
          optFormId = *,
          optGroupFromId = *,
          optGroupToId = *,
          optEmailTemplateId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal(1, Seq(relation))))

      when(
        fixture.userService.listByGroupId(
          groupId = eqTo(relation.groupFrom.id),
          includeDeleted = eqTo(false)
        )(*)
      ).thenReturn(toSuccessResult(ListWithTotal(1, Seq(user))))

      val context = Map("example" -> "any")
      val renderedSubject = "subject"
      val renderedBody = "body"

      when(fixture.templateEngineService.getContext(user, Some(event))).thenReturn(context)
      when(fixture.templateEngineService.render(template.subject, context)).thenReturn(renderedSubject)
      when(fixture.templateEngineService.render(template.body, context)).thenReturn(renderedBody)

      wait(fixture.service.execute(job))
      verify(fixture.mailService, times(2)).send(renderedSubject, user, renderedBody)
    }
  }
}
