package services

import java.sql.Timestamp

import models.assessment.{Answer, Assessment}
import models.dao.{AnswerDao, EventDao, GroupDao, ProjectRelationDao}
import models.event.Event
import models.form.Form
import models.project.Relation
import models.user.UserShort
import models.{ListWithTotal, NamedEntity}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalacheck.Gen
import testutils.fixture._
import utils.errors.{ApplicationError, BadRequestError, ConflictError, NotFoundError}
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
        optGroupFromIds = eqTo(Some(userGroupsIds)),
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
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
        optGroupFromIds = eqTo(Some(userGroupsIds)),
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal[Event](1, Seq(event))))

      when(fixture.relationDao.getList(
        optId = any[Option[Long]],
        optProjectId = eqTo(Some(projectId)),
        optKind = any[Option[Relation.Kind]]
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal(2, relations)))

      when(fixture.userService.listByGroupId(eqTo(relations(0).groupTo.get.id), eqTo(false))(any[ListMeta]))
        .thenReturn(EitherT.eitherT(toFuture(ListWithTotal(1, Seq(assessedUser)).right[ApplicationError])))

      when(fixture.formService.getOrCreateFreezedForm(event.id, relations(0).form.id))
        .thenReturn(EitherT.eitherT(toFuture(Forms(0).right[ApplicationError])))

      when(fixture.formService.getOrCreateFreezedForm(event.id, relations(1).form.id))
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

  "submit" should {
    "return error if event not found" in {
      val fixture = getFixture

      val user = UserFixture.user
      val event = Events(0)
      val projectId = 3
      val userGroupsIds = Seq(1L, 2, 3)
      val assessment = Assessment(None, Seq(Answer.Form(NamedEntity(1), Set())))

      when(fixture.groupDao.findGroupIdsByUserId(user.id)).thenReturn(toFuture(userGroupsIds))
      when(fixture.eventDao.getList(
        optId = eqTo(Some(event.id)),
        optStatus = any[Option[Event.Status]],
        optProjectId = eqTo(Some(projectId)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = eqTo(Some(userGroupsIds)),
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal[Event](0, Nil)))

      val result = wait(fixture.service.submit(event.id, projectId, assessment)(user).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[NotFoundError]
    }

    "return error if cant revote" in {
      val fixture = getFixture

      val user = UserFixture.user
      val event = Events(0).copy(canRevote = false)
      val projectId = 3
      val userGroupsIds = Seq(1L, 2, 3)
      val formId = 1
      val answer = Answer.Form(NamedEntity(formId), Set())
      val assessment = Assessment(None, Seq(answer))

      when(fixture.groupDao.findGroupIdsByUserId(user.id)).thenReturn(toFuture(userGroupsIds))
      when(fixture.eventDao.getList(
        optId = eqTo(Some(event.id)),
        optStatus = any[Option[Event.Status]],
        optProjectId = eqTo(Some(projectId)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = eqTo(Some(userGroupsIds)),
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal(1, Seq(event))))

      when(fixture.answerDao.getAnswer(event.id, projectId, user.id, None, formId))
        .thenReturn(toFuture(Some(answer)))

      val result = wait(fixture.service.submit(event.id, projectId, assessment)(user).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "return error if selfvoting" in {
      val fixture = getFixture

      val user = UserFixture.user
      val event = Events(0)
      val projectId = 3
      val userGroupsIds = Seq(1L, 2, 3)
      val formId = 1
      val answer = Answer.Form(NamedEntity(formId), Set())
      val assessment = Assessment(Some(UserShort.fromUser(user)), Seq(answer))

      when(fixture.groupDao.findGroupIdsByUserId(user.id)).thenReturn(toFuture(userGroupsIds))
      when(fixture.eventDao.getList(
        optId = eqTo(Some(event.id)),
        optStatus = any[Option[Event.Status]],
        optProjectId = eqTo(Some(projectId)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = eqTo(Some(userGroupsIds)),
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal(1, Seq(event))))

      when(fixture.answerDao.getAnswer(event.id, projectId, user.id, Some(user.id), formId))
        .thenReturn(toFuture(None))

      val result = wait(fixture.service.submit(event.id, projectId, assessment)(user).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[BadRequestError]
    }

    "return error if can't validate form" in {
      val baseForm = Form(1, "", Seq(), Form.Kind.Freezed)
      val invalidFormsWithAnswers = Seq(
        // Duplicate answers
        baseForm ->
          Answer.Form(NamedEntity(1), Set(Answer.Element(2, Some(""), None), Answer.Element(2, Some(""), None))),
        // Missed required answer
        baseForm.copy(elements = Seq(Form.Element(1, Form.ElementKind.TextArea, "", required = true, Nil))) ->
          Answer.Form(NamedEntity(1), Set()),
        // Values elements contains text
        baseForm.copy(elements = Seq(Form.Element(1, Form.ElementKind.Select, "", required = true, Nil))) ->
          Answer.Form(NamedEntity(1), Set(Answer.Element(1, Some(""), None))),
        // Text element contains values answer
        baseForm.copy(elements = Seq(Form.Element(1, Form.ElementKind.TextArea, "", required = true, Nil))) ->
          Answer.Form(NamedEntity(1), Set(Answer.Element(1, None, Some(Seq(1))))),
        // Values answer contains unknown element
        baseForm.copy(elements = Seq(Form.Element(1, Form.ElementKind.Select, "", required = true, Seq(Form.ElementValue(3, ""))))) ->
          Answer.Form(NamedEntity(1), Set(Answer.Element(1, None, Some(Seq(1))))),
        // Text answer is missed
        baseForm.copy(elements = Seq(Form.Element(1, Form.ElementKind.TextArea, "", required = true, Nil))) ->
          Answer.Form(NamedEntity(1), Set(Answer.Element(1, None, None))),
        // Answer for unknown element id
        baseForm.copy(elements = Seq(Form.Element(1, Form.ElementKind.TextArea, "", required = false, Nil))) ->
          Answer.Form(NamedEntity(1), Set(Answer.Element(2, Some(""), None)))
      )

      forAll(Gen.oneOf(invalidFormsWithAnswers)) { case (form, answer) =>
        val fixture = getFixture

        val user = UserFixture.user
        val event = Events(0)
        val projectId = 3
        val userGroupsIds = Seq(1L, 2, 3)
        val formId = answer.form.id
        val assessment = Assessment(None, Seq(answer))

        when(fixture.groupDao.findGroupIdsByUserId(user.id)).thenReturn(toFuture(userGroupsIds))
        when(fixture.eventDao.getList(
          optId = eqTo(Some(event.id)),
          optStatus = any[Option[Event.Status]],
          optProjectId = eqTo(Some(projectId)),
          optNotificationFrom = any[Option[Timestamp]],
          optNotificationTo = any[Option[Timestamp]],
          optFormId = any[Option[Long]],
          optGroupFromIds = eqTo(Some(userGroupsIds)),
          optEndFrom = any[Option[Timestamp]],
          optEndTimeTo = any[Option[Timestamp]]
        )(any[ListMeta]))
          .thenReturn(toFuture(ListWithTotal(1, Seq(event))))

        when(fixture.answerDao.getAnswer(event.id, projectId, user.id, None, formId))
          .thenReturn(toFuture(None))

        when(fixture.formService.getById(formId))
          .thenReturn(EitherT.eitherT(toFuture(\/-(form): ApplicationError \/ Form)))

        val result = wait(fixture.service.submit(event.id, projectId, assessment)(user).run)

        result mustBe 'left
      }
    }

    "return error if there is no available relations for user" in {
      val fixture = getFixture

      val user = UserFixture.user
      val event = Events(0)
      val projectId = 3
      val userGroupsIds = Seq(1L, 2, 3)
      val form = Form(1, "", Seq(Form.Element(1, Form.ElementKind.TextField, "", true, Nil)), Form.Kind.Freezed)
      val answer = Answer.Form(NamedEntity(form.id), Set(Answer.Element(1, Some("text"), None)))
      val assessment = Assessment(None, Seq(answer))

      when(fixture.groupDao.findGroupIdsByUserId(user.id)).thenReturn(toFuture(userGroupsIds))
      when(fixture.eventDao.getList(
        optId = eqTo(Some(event.id)),
        optStatus = any[Option[Event.Status]],
        optProjectId = eqTo(Some(projectId)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = eqTo(Some(userGroupsIds)),
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal(1, Seq(event))))

      when(fixture.answerDao.getAnswer(event.id, projectId, user.id, None, form.id))
        .thenReturn(toFuture(None))

      when(fixture.formService.getById(form.id))
        .thenReturn(EitherT.eitherT(toFuture(\/-(form): ApplicationError \/ Form)))

      when(fixture.relationDao.getList(
        optId = any[Option[Long]],
        optProjectId = eqTo(Some(projectId)),
        optKind = eqTo(Some(Relation.Kind.Survey))
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Relation](0, Nil)))

      val result = wait(fixture.service.submit(event.id, projectId, assessment)(user).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[ConflictError.Assessment.WrongParameters.type]
    }

    "return error if user not in from groups" in {
      val fixture = getFixture

      val user = UserFixture.user
      val event = Events(0)
      val projectId = 3
      val userGroupsIds = Seq(1L, 2, 3)
      val form = Form(1, "", Seq(Form.Element(1, Form.ElementKind.TextField, "", true, Nil)), Form.Kind.Freezed)
      val answer = Answer.Form(NamedEntity(form.id), Set(Answer.Element(1, Some("text"), None)))
      val assessment = Assessment(None, Seq(answer))
      val relation = Relation(
        id = 1,
        project = NamedEntity(2),
        groupFrom = NamedEntity(30),
        groupTo = Some(NamedEntity(4)),
        form = NamedEntity(5),
        kind = Relation.Kind.Classic,
        templates = Nil
      )

      when(fixture.groupDao.findGroupIdsByUserId(user.id)).thenReturn(toFuture(userGroupsIds))
      when(fixture.eventDao.getList(
        optId = eqTo(Some(event.id)),
        optStatus = any[Option[Event.Status]],
        optProjectId = eqTo(Some(projectId)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = eqTo(Some(userGroupsIds)),
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal(1, Seq(event))))

      when(fixture.answerDao.getAnswer(event.id, projectId, user.id, None, form.id))
        .thenReturn(toFuture(None))

      when(fixture.formService.getById(form.id))
        .thenReturn(EitherT.eitherT(toFuture(\/-(form): ApplicationError \/ Form)))

      when(fixture.relationDao.getList(
        optId = any[Option[Long]],
        optProjectId = eqTo(Some(projectId)),
        optKind = eqTo(Some(Relation.Kind.Survey))
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Relation](1, Seq(relation))))

      when(fixture.formService.getOrCreateFreezedForm(event.id, relation.form.id))
        .thenReturn(EitherT.eitherT(toFuture(\/-(form): ApplicationError \/ Form)))

      val result = wait(fixture.service.submit(event.id, projectId, assessment)(user).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[ConflictError.Assessment.WrongParameters.type]
    }

    "save answer in db" in {
      val fixture = getFixture

      val user = UserFixture.user
      val event = Events(0)
      val projectId = 3
      val userGroupsIds = Seq(1L, 2, 3)
      val form = Form(1, "", Seq(Form.Element(1, Form.ElementKind.TextField, "", true, Nil)), Form.Kind.Freezed)
      val answer = Answer.Form(NamedEntity(form.id), Set(Answer.Element(1, Some("text"), None)))
      val assessment = Assessment(None, Seq(answer))
      val relation = Relation(
        id = 1,
        project = NamedEntity(2),
        groupFrom = NamedEntity(3),
        groupTo = None,
        form = NamedEntity(5),
        kind = Relation.Kind.Classic,
        templates = Nil
      )

      when(fixture.groupDao.findGroupIdsByUserId(user.id)).thenReturn(toFuture(userGroupsIds))
      when(fixture.eventDao.getList(
        optId = eqTo(Some(event.id)),
        optStatus = any[Option[Event.Status]],
        optProjectId = eqTo(Some(projectId)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = eqTo(Some(userGroupsIds)),
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal(1, Seq(event))))

      when(fixture.answerDao.getAnswer(event.id, projectId, user.id, None, form.id))
        .thenReturn(toFuture(None))

      when(fixture.formService.getById(form.id))
        .thenReturn(EitherT.eitherT(toFuture(\/-(form): ApplicationError \/ Form)))

      when(fixture.relationDao.getList(
        optId = any[Option[Long]],
        optProjectId = eqTo(Some(projectId)),
        optKind = eqTo(Some(Relation.Kind.Survey))
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Relation](1, Seq(relation))))

      when(fixture.formService.getOrCreateFreezedForm(event.id, relation.form.id))
        .thenReturn(EitherT.eitherT(toFuture(\/-(form): ApplicationError \/ Form)))

      when(fixture.answerDao.saveAnswer(event.id, projectId, user.id, None, answer))
        .thenReturn(toFuture(answer))

      val result = wait(fixture.service.submit(event.id, projectId, assessment)(user).run)

      result mustBe 'right
    }
  }
}
