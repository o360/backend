package services

import models.assessment.Answer
import models.dao.{AnswerDao, ProjectDao, ProjectRelationDao}
import models.form.Form
import models.project.Relation
import models.report.{AggregatedReport, Report}
import models.{ListWithTotal, NamedEntity}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import testutils.fixture.{FormFixture, ProjectFixture, UserFixture}
import utils.errors.ApplicationError
import utils.listmeta.ListMeta

import scalaz.{EitherT, \/, \/-}

/**
  * Test for report service.
  */
class ReportServiceTest extends BaseServiceTest with FormFixture with UserFixture with ProjectFixture {


  private case class Fixture(
    userService: UserService,
    relationDao: ProjectRelationDao,
    formService: FormService,
    answerDao: AnswerDao,
    service: ReportService
  )

  private def getFixture = {
    val userService = mock[UserService]
    val relationDao = mock[ProjectRelationDao]
    val formService = mock[FormService]
    val answerDao = mock[AnswerDao]
    val service = new ReportService(userService, relationDao, formService, answerDao)
    Fixture(userService, relationDao, formService, answerDao, service)
  }

  "getReport" should {
    "return report" in {
      val fixture = getFixture

      val eventId = 1
      val projectId = 2
      val groupFromId = 3L
      val groupToId = 4L
      val formTemplateId = 5
      val freezedForm = Forms(0).copy(id = 6)
      val userFrom = Users(0).copy(id = 7)
      val userTo = Users(1).copy(id = 8)

      val relation = Relation(
        id = 1,
        project = NamedEntity(projectId),
        groupFrom = NamedEntity(groupFromId),
        groupTo = Some(NamedEntity(groupToId)),
        form = NamedEntity(formTemplateId),
        kind = Relation.Kind.Classic,
        templates = Nil,
        hasInProgressEvents = false,
        canSelfVote = false
      )

      val answer = Answer.Form(
        NamedEntity(freezedForm.id),
        Set(Answer.Element(freezedForm.elements(0).id, Some("text"), None))
      )

      when(fixture.relationDao.getList(
        optId = any[Option[Long]],
        optProjectId = eqTo(Some(projectId)),
        optKind = any[Option[Relation.Kind]],
        optFormId = any[Option[Long]],
        optGroupFromId = any[Option[Long]],
        optGroupToId = any[Option[Long]],
        optEmailTemplateId = any[Option[Long]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal(1, Seq(relation))))

      when(fixture.userService.getGroupIdToUsersMap(Seq(groupFromId, groupToId), true))
        .thenReturn(toFuture(Map(groupFromId -> Seq(userFrom), groupToId -> Seq(userTo))))

      when(fixture.formService.getOrCreateFreezedForm(eventId, formTemplateId))
        .thenReturn(EitherT.eitherT(toFuture(\/-(freezedForm): ApplicationError \/ Form)))

      when(fixture.answerDao.getAnswer(eventId, projectId, userFrom.id, Some(userTo.id), freezedForm.id))
        .thenReturn(toFuture(Some(answer)))

      val result = wait(fixture.service.getReport(eventId, projectId))

      val expectedResult =
        Seq(Report(
          Some(userTo),
          Seq(Report.FormReport(
            freezedForm,
            Seq(
              Report.FormElementReport(freezedForm.elements(0),
                Seq(Report.FormElementAnswerReport(userFrom, answer.answers.head, false))),
              Report.FormElementReport(freezedForm.elements(1), Seq())
            )))))

      result mustBe expectedResult
    }
  }

  "getAggregatedReport" should {
    "return aggregated report" in {
      val fixture = getFixture
      val elementsWithAnswers = Seq(
        Form.Element(1, Form.ElementKind.TextArea, "", false, Nil) ->
          Answer.Element(1, Some("text"), None),
        Form.Element(2, Form.ElementKind.Checkbox, "", false, Nil) ->
          Answer.Element(2, Some("true"), None),
        Form.Element(3, Form.ElementKind.Radio, "", false, Seq(Form.ElementValue(10, "radioval"))) ->
          Answer.Element(3, None, Some(Set(10)))
      )
      val form = Forms(0).copy(id = 1, elements = elementsWithAnswers.map(_._1))
      val userTo = Users(1).copy(id = 2)
      val userFrom = Users(0).copy(id = 3)
      val answer = Answer.Form(
        NamedEntity(form.id),
        elementsWithAnswers.map(_._2).toSet
      )

      val report =
        Report(
          Some(userTo),
          Seq(Report.FormReport(
            form,
            elementsWithAnswers.map { case (element, answerElement) =>
              Report.FormElementReport(element, Seq(Report.FormElementAnswerReport(userFrom, answerElement, false)))
            }
          )))

      val result = fixture.service.getAggregatedReport(report)

      val expectedResult = AggregatedReport(
        Some(userTo),
        Seq(AggregatedReport.FormAnswer(
          form,
          Seq(
            AggregatedReport.FormElementAnswer(form.elements(0), "total: 1"),
            AggregatedReport.FormElementAnswer(form.elements(1), """"true" - 1 (100.00%)"""),
            AggregatedReport.FormElementAnswer(form.elements(2), """"radioval" - 1 (100.00%)""")
          ))))

      result mustBe expectedResult
    }
  }
}
