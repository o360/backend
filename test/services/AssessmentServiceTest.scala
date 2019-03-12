package services

import models.assessment.{Answer, Assessment, PartialAnswer, PartialAssessment}
import models.dao._
import models.form.Form
import models.form.element._
import models.project.ActiveProject
import models.{ListWithTotal, NamedEntity}
import org.mockito.Mockito._
import org.scalacheck.Gen
import services.event.ActiveProjectService
import testutils.fixture._
import utils.errors.BadRequestError.Assessment.WithUserFormInfo
import utils.errors.{ConflictError, NotFoundError}

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
    userDao: UserDao,
    answerDao: AnswerDao,
    activeProjectService: ActiveProjectService,
    eventDao: EventDao,
    service: AssessmentService
  )

  private def getFixture = {
    val formService = mock[FormService]
    val userDao = mock[UserDao]
    val answerDao = mock[AnswerDao]
    val activeProjectService = mock[ActiveProjectService]
    val eventDao = mock[EventDao]
    val service = new AssessmentService(formService, userDao, answerDao, activeProjectService, eventDao, ec)
    Fixture(formService, userDao, answerDao, activeProjectService, eventDao, service)
  }

  "getList" should {
    "return error if there is no available assessment objects" in {
      val fixture = getFixture

      val user = UserFixture.user
      val activeProjectId: Long = 1

      when(fixture.activeProjectService.getById(activeProjectId)(user))
        .thenReturn(toErrorResult[ActiveProject](NotFoundError.ActiveProject(activeProjectId)))

      val result = wait(fixture.service.getList(activeProjectId)(user).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[NotFoundError]
    }

    "return available assessment objects" in {
      val fixture = getFixture

      val user = UserFixture.user
      val activeProject = ActiveProjectFixture.values(0)

      when(fixture.activeProjectService.getById(activeProject.id)(user))
        .thenReturn(toSuccessResult(activeProject))
      when(
        fixture.answerDao.getList(
          optEventId = *,
          optActiveProjectId = eqTo(Some(activeProject.id)),
          optUserFromId = eqTo(Some(user.id)),
          optFormId = *,
          optUserToId = *,
        )).thenReturn(toFuture(Answers))
      when(
        fixture.userDao.getList(
          optIds = eqTo(Some(Seq(3L))),
          optRole = *,
          optStatus = *,
          optGroupIds = *,
          optName = *,
          optEmail = *,
          optProjectIdAuditor = *,
          includeDeleted = eqTo(true),
        )(*)).thenReturn(toFuture(ListWithTotal(Seq(user.copy(id = 3)))))

      val result = wait(fixture.service.getList(activeProject.id)(user).run)

      result mustBe 'right
      result.toOption.get mustBe ListWithTotal(
        Seq(
          Assessment(Some(user.copy(id = 3)), Seq(Answers(0))),
          Assessment(None, Seq(Answers(1))),
        )
      )
    }
  }

  "bulkSubmit" should {
    "return error if project not found" in {
      val fixture = getFixture

      val user = UserFixture.user
      val activeProjectId: Long = 1

      when(fixture.activeProjectService.getById(activeProjectId)(user))
        .thenReturn(toErrorResult[ActiveProject](NotFoundError.ActiveProject(activeProjectId)))

      val result = wait(fixture.service.bulkSubmit(activeProjectId, Seq())(user).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[NotFoundError]
    }

    "return error if event not found" in {
      val fixture = getFixture
      val user = UserFixture.user
      val activeProject = ActiveProjectFixture.values(0)

      when(fixture.activeProjectService.getById(activeProject.id)(user))
        .thenReturn(toSuccessResult(activeProject))
      when(fixture.eventDao.findById(activeProject.eventId)).thenReturn(toFuture(None))

      val result = wait(fixture.service.bulkSubmit(activeProject.id, Seq())(user).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[NotFoundError]
    }

    "return error if event not in progress" in {
      val fixture = getFixture
      val user = UserFixture.user
      val activeProject = ActiveProjectFixture.values(0)
      val event = Events(1)

      when(fixture.activeProjectService.getById(activeProject.id)(user))
        .thenReturn(toSuccessResult(activeProject))
      when(fixture.eventDao.findById(activeProject.eventId)).thenReturn(toFuture(Some(event)))

      val result = wait(fixture.service.bulkSubmit(activeProject.id, Seq())(user).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[ConflictError.Assessment.InactiveEvent.type]
    }

    "return error if there is no existed answer" in {
      val fixture = getFixture
      val user = UserFixture.user
      val activeProject = ActiveProjectFixture.values(0)
      val event = Events(3)
      val formId = 1
      val assessment = PartialAssessment(None, Seq(PartialAnswer(formId, false, Set())))

      when(fixture.activeProjectService.getById(activeProject.id)(user))
        .thenReturn(toSuccessResult(activeProject))
      when(fixture.eventDao.findById(activeProject.eventId)).thenReturn(toFuture(Some(event)))

      when(fixture.answerDao.getAnswer(activeProject.id, user.id, None, formId)).thenReturn(toFuture(None))

      val result = wait(fixture.service.bulkSubmit(activeProject.id, Seq(assessment))(user).run)

      result mustBe 'left
      result.swap.toOption.get.getInnerErrors.get.head mustBe a[WithUserFormInfo]
    }

    "return error if cant revote" in {
      val fixture = getFixture
      val user = UserFixture.user
      val activeProject = ActiveProjectFixture.values(0).copy(canRevote = false)
      val event = Events(3)
      val formId = 1
      val assessment = PartialAssessment(None, Seq(PartialAnswer(formId, false, Set())))
      val answer = Answer(activeProject.id, user.id, None, NamedEntity(formId), true, Answer.Status.Answered)

      when(fixture.activeProjectService.getById(activeProject.id)(user))
        .thenReturn(toSuccessResult(activeProject))
      when(fixture.eventDao.findById(activeProject.eventId)).thenReturn(toFuture(Some(event)))

      when(fixture.answerDao.getAnswer(activeProject.id, user.id, None, formId)).thenReturn(toFuture(Some(answer)))

      val result = wait(fixture.service.bulkSubmit(activeProject.id, Seq(assessment))(user).run)

      result mustBe 'left
      result.swap.toOption.get.getInnerErrors.get.head mustBe a[WithUserFormInfo]
    }

    "return error if cant skip" in {
      val fixture = getFixture
      val user = UserFixture.user
      val activeProject = ActiveProjectFixture.values(0)
      val event = Events(3)
      val formId = 1
      val assessment = PartialAssessment(None, Seq(PartialAnswer(formId, false, Set(), true)))
      val answer = Answer(activeProject.id, user.id, None, NamedEntity(formId), false, Answer.Status.New)

      when(fixture.activeProjectService.getById(activeProject.id)(user))
        .thenReturn(toSuccessResult(activeProject))
      when(fixture.eventDao.findById(activeProject.eventId)).thenReturn(toFuture(Some(event)))

      when(fixture.answerDao.getAnswer(activeProject.id, user.id, None, formId)).thenReturn(toFuture(Some(answer)))

      val result = wait(fixture.service.bulkSubmit(activeProject.id, Seq(assessment))(user).run)

      result mustBe 'left
      result.swap.toOption.get.getInnerErrors.get.head mustBe a[WithUserFormInfo]
    }

    "return error if can't validate form" in {
      val baseForm = Form(1, "", Seq(), Form.Kind.Freezed, true, "machine name")
      val createAnswer = Answer(1, 1, None, NamedEntity(1), false, Answer.Status.Answered, false, _: Set[Answer.Element])

      val createElement = Form.Element(1, _: ElementKind, "", _: Boolean, _: Seq[Form.ElementValue], Nil, "machine name", None)

      val invalidFormsWithAnswers = Seq(
        // Duplicate answers
        baseForm ->
          createAnswer(Set(Answer.Element(2, Some(""), None, None), Answer.Element(2, Some(""), None, None))),
        // Missed required answer
        baseForm.copy(elements = Seq(createElement(TextArea, true, Nil))) ->
          createAnswer(Set()),
        // Text element contains values answer
        baseForm.copy(elements = Seq(createElement(TextArea, true, Nil))) ->
          createAnswer(Set(Answer.Element(1, None, Some(Set(1)), None))),
        // Values answer contains unknown element
        baseForm.copy(elements =
          Seq(createElement(Select, true, Seq(Form.ElementValue(3, ""))))) ->
          createAnswer(Set(Answer.Element(1, None, Some(Set(1)), None))),
        // Text answer is missed
        baseForm.copy(elements = Seq(createElement(TextArea, true, Nil))) ->
          createAnswer(Set(Answer.Element(1, None, None, None))),
        // Answer for unknown element id
        baseForm.copy(elements = Seq(createElement(TextArea, false, Nil))) ->
          createAnswer(Set(Answer.Element(2, Some(""), None, None)))
      )

      forAll(Gen.oneOf(invalidFormsWithAnswers)) {
        case (form, answer) =>
          val fixture = getFixture
          val user = UserFixture.user
          val activeProject = ActiveProjectFixture.values(0)
          val event = Events(3)

          val dummyAnswer = Answer(1, 1, None, NamedEntity(1), true)

          when(fixture.activeProjectService.getById(activeProject.id)(user))
            .thenReturn(toSuccessResult(activeProject))
          when(fixture.eventDao.findById(activeProject.eventId)).thenReturn(toFuture(Some(event)))

          when(fixture.answerDao.getAnswer(activeProject.id, user.id, None, form.id))
            .thenReturn(toFuture(Some(dummyAnswer)))
          when(fixture.formService.getById(form.id)).thenReturn(toSuccessResult(form))

          val assessment = PartialAssessment(None, Seq(PartialAnswer(form.id, false, answer.elements)))
          val result = wait(fixture.service.bulkSubmit(activeProject.id, Seq(assessment))(user).run)
          result mustBe 'left
      }
    }

    "save answer in db" in {
      val fixture = getFixture
      val user = UserFixture.user
      val activeProject = ActiveProjectFixture.values(0)
      val event = Events(3)

      val form =
        Form(1, "", Seq(Form.Element(1, TextField, "", false, Nil, Nil, "", None)), Form.Kind.Freezed, true, "")

      val answer = Answer(activeProject.id, user.id, None, NamedEntity(form.id), true, Answer.Status.Answered)
      val assessment = PartialAssessment(None, Seq(PartialAnswer(form.id, false, Set())))

      when(fixture.activeProjectService.getById(activeProject.id)(user))
        .thenReturn(toSuccessResult(activeProject))
      when(fixture.eventDao.findById(activeProject.eventId)).thenReturn(toFuture(Some(event)))

      when(fixture.answerDao.getAnswer(activeProject.id, user.id, None, form.id)).thenReturn(toFuture(Some(answer)))
      when(fixture.formService.getById(form.id)).thenReturn(toSuccessResult(form))
      when(fixture.answerDao.saveAnswer(answer)).thenReturn(toFuture(answer))

      val result = wait(fixture.service.bulkSubmit(activeProject.id, Seq(assessment))(user).run)

      result mustBe 'right
    }
  }
}
