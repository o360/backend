package services

import java.sql.Timestamp

import models.{ListWithTotal, NamedEntity}
import models.dao.{EventDao, ProjectDao, ProjectRelationDao, TemplateDao}
import models.event.Event
import models.notification.Notification
import models.project.{Project, Relation, TemplateBinding}
import org.mockito.ArgumentMatchers.any
import testutils.generator.{EventGenerator, ListWithTotalGenerator}
import utils.listmeta.ListMeta
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import testutils.fixture.{EventFixture, ProjectFixture, ProjectRelationFixture, UserFixture}
import utils.errors.ApplicationError

import scalaz._
import Scalaz.ToEitherOps

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
    with ProjectRelationFixture
{

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
    val service = new NotificationService(eventDao, projectDao, relationDao, templateDao, userService, mailService, templateEngineService)
    Fixture(eventDao, projectDao, relationDao, templateDao, userService, mailService, templateEngineService, service)
  }

  private val from = new Timestamp(0)
  private val between = new Timestamp(100)
  private val to = new Timestamp(Long.MaxValue)

  "sendEventsNotifications" should {
    "do nothing if there is no events" in {
      val fixture = getFixture

      when(fixture.eventDao.getList(
        optId = any[Option[Long]],
        optStatus = any[Option[Event.Status]],
        optProjectId = any[Option[Long]],
        optNotificationFrom = eqTo(Some(from)),
        optNotificationTo = eqTo(Some(to)),
        optFormId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal[Event](0, Nil)))

      wait(fixture.service.sendEventsNotifications(from, to))
      succeed
    }

    "query projects for each event" in {
      val fixture = getFixture
      val event = Events(0)

      when(fixture.eventDao.getList(
        optId = any[Option[Long]],
        optStatus = any[Option[Event.Status]],
        optProjectId = any[Option[Long]],
        optNotificationFrom = eqTo(Some(from)),
        optNotificationTo = eqTo(Some(to)),
        optFormId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal(1, Seq(event))))

      when(fixture.projectDao.getList(
        optId = any[Option[Long]],
        optEventId = eqTo(Some(event.id)),
        optGroupFromIds = any[Option[Seq[Long]]],
        optFormId = any[Option[Long]],
        optGroupAuditorId = any[Option[Long]],
        optEmailTemplateId = any[Option[Long]]
      )(any[ListMeta]))
      .thenReturn(toFuture(ListWithTotal[Project](0, Nil)))

      wait(fixture.service.sendEventsNotifications(from, to))
      succeed
    }

    "send emails to auditor" in {
      val fixture = getFixture
      val event = Events(0).copy(
        notifications = Seq(Event.NotificationTime(
          between,
          Notification.Kind.Begin,
          Notification.Recipient.Auditor
        )))
      val project = Projects(0).copy(
        templates = Seq(TemplateBinding(
          NamedEntity(1, "template name"),
          Notification.Kind.Begin,
          Notification.Recipient.Auditor
        )))
      val template = Templates(0)
      val user = Users(0)

      when(fixture.eventDao.getList(
        optId = any[Option[Long]],
        optStatus = any[Option[Event.Status]],
        optProjectId = any[Option[Long]],
        optNotificationFrom = eqTo(Some(from)),
        optNotificationTo = eqTo(Some(to)),
        optFormId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal(1, Seq(event))))

      when(fixture.projectDao.getList(
        optId = any[Option[Long]],
        optEventId = eqTo(Some(event.id)),
        optGroupFromIds = any[Option[Seq[Long]]],
        optFormId = any[Option[Long]],
        optGroupAuditorId = any[Option[Long]],
        optEmailTemplateId = any[Option[Long]]
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal(1, Seq(project))))

      when(fixture.templateDao.findById(1)).thenReturn(toFuture(Some(template)))

      when(fixture.relationDao.getList(
        optId = any[Option[Long]],
        optProjectId = eqTo(Some(project.id)),
        optKind = any[Option[Relation.Kind]],
        optFormId = any[Option[Long]],
        optGroupFromId = any[Option[Long]],
        optGroupToId = any[Option[Long]],
        optEmailTemplateId = any[Option[Long]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Relation](0, Nil)))

      when(fixture.userService.listByGroupId(
        groupId = eqTo(project.groupAuditor.id),
        includeDeleted = eqTo(false)
      )(any[ListMeta]))
        .thenReturn(EitherT.eitherT(toFuture(ListWithTotal(1, Seq(user)).right[ApplicationError])))

      val context = Map("example" -> "any")
      val renderedSubject = "subject"
      val renderedBody = "body"

      when(fixture.templateEngineService.getContext(user, event)).thenReturn(context)
      when(fixture.templateEngineService.render(template.subject, context)).thenReturn(renderedSubject)
      when(fixture.templateEngineService.render(template.body, context)).thenReturn(renderedBody)

      wait(fixture.service.sendEventsNotifications(from, to))
      verify(fixture.mailService, times(1)).send(renderedSubject, user, renderedBody)
    }

    "send emails to respondent" in {
      val fixture = getFixture
      val event = Events(0).copy(
        notifications = Seq(Event.NotificationTime(
          between,
          Notification.Kind.Begin,
          Notification.Recipient.Respondent
        )))
      val project = Projects(0).copy(
        templates = Seq(TemplateBinding(
          NamedEntity(1, "template name"),
          Notification.Kind.Begin,
          Notification.Recipient.Respondent
        )))
      val template = Templates(0)
      val user = Users(0)
      val relation = ProjectRelations(0).copy(
        templates = Seq(TemplateBinding(
          NamedEntity(1, "template name"),
          Notification.Kind.Begin,
          Notification.Recipient.Respondent
        )))

      when(fixture.eventDao.getList(
        optId = any[Option[Long]],
        optStatus = any[Option[Event.Status]],
        optProjectId = any[Option[Long]],
        optNotificationFrom = eqTo(Some(from)),
        optNotificationTo = eqTo(Some(to)),
        optFormId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal(1, Seq(event))))

      when(fixture.projectDao.getList(
        optId = any[Option[Long]],
        optEventId = eqTo(Some(event.id)),
        optGroupFromIds = any[Option[Seq[Long]]],
        optFormId = any[Option[Long]],
        optGroupAuditorId = any[Option[Long]],
        optEmailTemplateId = any[Option[Long]]
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal(1, Seq(project))))

      when(fixture.templateDao.findById(1)).thenReturn(toFuture(Some(template)))

      when(fixture.relationDao.getList(
        optId = any[Option[Long]],
        optProjectId = eqTo(Some(project.id)),
        optKind = any[Option[Relation.Kind]],
        optFormId = any[Option[Long]],
        optGroupFromId = any[Option[Long]],
        optGroupToId = any[Option[Long]],
        optEmailTemplateId = any[Option[Long]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal(1, Seq(relation))))

      when(fixture.userService.listByGroupId(
        groupId = eqTo(relation.groupFrom.id),
        includeDeleted = eqTo(false)
      )(any[ListMeta]))
        .thenReturn(EitherT.eitherT(toFuture(ListWithTotal(1, Seq(user)).right[ApplicationError])))

      val context = Map("example" -> "any")
      val renderedSubject = "subject"
      val renderedBody = "body"

      when(fixture.templateEngineService.getContext(user, event)).thenReturn(context)
      when(fixture.templateEngineService.render(template.subject, context)).thenReturn(renderedSubject)
      when(fixture.templateEngineService.render(template.body, context)).thenReturn(renderedBody)

      wait(fixture.service.sendEventsNotifications(from, to))
      verify(fixture.mailService, times(2)).send(renderedSubject, user, renderedBody)
    }
  }
}



