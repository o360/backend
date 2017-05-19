package services

import java.sql.Timestamp

import models.assessment.{Answer, Assessment}
import models.dao.{AnswerDao, EventDao, GroupDao, ProjectRelationDao}
import models.event.Event
import models.project.Relation
import models.user.UserShort
import models.{ListWithTotal, NamedEntity}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import testutils.fixture._
import utils.errors.{ApplicationError, NotFoundError}
import utils.listmeta.ListMeta

import scalaz.Scalaz.ToEitherOps
import scalaz._

/**
  * Test for assessment service.
  */
class AssessmentServiceTest
  extends BaseServiceTest
    with EventFixture
    with ProjectRelationFixture
    with UserFixture
    with FormFixture
    with AnswerFixture {

  private case class Fixture(
    formService: FormService,
    userService: UserService,
    groupDao: GroupDao,
    eventDao: EventDao,
    relationDao: ProjectRelationDao,
    answerDao: AnswerDao,
    service: AssessmentService
  )

  private def getFixture = {
    val formService = mock[FormService]
    val userService = mock[UserService]
    val groupDao = mock[GroupDao]
    val eventDao = mock[EventDao]
    val relationDao = mock[ProjectRelationDao]
    val answerDao = mock[AnswerDao]
    val service = new AssessmentService(formService, userService, groupDao, eventDao, relationDao, answerDao)
    Fixture(formService, userService, groupDao, eventDao, relationDao, answerDao, service)
  }

  "getList" should {
    "return error if there is no available assessment objects" in {
      val fixture = getFixture

      val user = UserFixture.user
      val event = Events(0)
      val projectId = 3
      val userGroupsIds = Seq(1L, 2, 3)

      when(fixture.groupDao.findGroupIdsByUserId(user.id)).thenReturn(toFuture(userGroupsIds))
      when(fixture.eventDao.getList(
        optId = eqTo(Some(event.id)),
        optStatus = any[Option[Event.Status]],
        optProjectId = eqTo(Some(projectId)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = eqTo(Some(userGroupsIds))
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal[Event](0, Nil)))

      val result = wait(fixture.service.getList(event.id, projectId)(user).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[NotFoundError]
    }

    "return available assessment objects" in {
      val fixture = getFixture

      val user = UserFixture.user
      val assessedUser = Users(1)
      val event = Events(0)
      val projectId = 3
      val userGroupsIds = Seq(1L, 2, 3)
      val relations = Seq(
        ProjectRelations(0).copy(
          groupFrom = NamedEntity(1),
          kind = Relation.Kind.Classic,
          groupTo = Some(NamedEntity(4)),
          form = NamedEntity(10)
        ),
        ProjectRelations(1).copy(
          groupFrom = NamedEntity(2),
          kind = Relation.Kind.Survey,
          groupTo = None,
          form = NamedEntity(11)
        )
      )

      val answer = Answers(0)


      when(fixture.groupDao.findGroupIdsByUserId(user.id)).thenReturn(toFuture(userGroupsIds))
      when(fixture.eventDao.getList(
        optId = eqTo(Some(event.id)),
        optStatus = any[Option[Event.Status]],
        optProjectId = eqTo(Some(projectId)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = eqTo(Some(userGroupsIds))
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal[Event](1, Seq(event))))

      when(fixture.relationDao.getList(optId = any[Option[Long]], optProjectId = eqTo(Some(projectId)))(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal(2, relations)))

      when(fixture.userService.listByGroupId(eqTo(relations(0).groupTo.get.id))(any[ListMeta]))
        .thenReturn(EitherT.eitherT(toFuture(ListWithTotal(1, Seq(assessedUser)).right[ApplicationError])))

      when(fixture.formService.getOrCreateFreezedForm(event.id, relations(0).form.id)(user))
        .thenReturn(EitherT.eitherT(toFuture(Forms(0).right[ApplicationError])))

      when(fixture.formService.getOrCreateFreezedForm(event.id, relations(1).form.id)(user))
        .thenReturn(EitherT.eitherT(toFuture(Forms(1).right[ApplicationError])))

      when(fixture.answerDao.getAnswer(event.id, projectId, user.id, Some(assessedUser.id), Forms(0).id))
        .thenReturn(toFuture(None))

      when(fixture.answerDao.getAnswer(event.id, projectId, user.id, None, Forms(1).id))
        .thenReturn(toFuture(Some(answer)))

      val result = wait(fixture.service.getList(event.id, projectId)(user).run)

      result mustBe 'right
      result.toOption.get mustBe ListWithTotal(2, Seq(
        Assessment(None, Seq(answer)),
        Assessment(Some(UserShort.fromUser(assessedUser)), Seq(Answer.Form(NamedEntity(Forms(0).id, Forms(0).name), Set())))
      ))
    }
  }
}
