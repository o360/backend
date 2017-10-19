package services

import models.assessment.Answer
import models.dao.{AnswerDao, UserDao}
import models.form.Form
import models.form.element._
import models.report.{AggregatedReport, Report, SimpleReport}
import models.user.User
import models.{ListWithTotal, NamedEntity}
import org.mockito.Mockito._
import testutils.fixture.{FormFixture, ProjectFixture, UserFixture}
import utils.errors.AuthorizationError

/**
  * Test for report service.
  */
class ReportServiceTest extends BaseServiceTest with FormFixture with UserFixture with ProjectFixture {

  private case class Fixture(
    userDao: UserDao,
    formService: FormService,
    answerDao: AnswerDao,
    service: ReportService
  )

  private def getFixture = {
    val userDao = mock[UserDao]
    val formService = mock[FormService]
    val answerDao = mock[AnswerDao]
    val service = new ReportService(userDao, formService, answerDao, ec)
    Fixture(userDao, formService, answerDao, service)
  }

  "getReport" should {
    "return report" in {
      val fixture = getFixture

      val activeProjectId = 1
      val form = Forms(0).copy(id = 2)

      val userFrom = Users(0)
      val userTo = Users(1)

      val answer = Answer(
        activeProjectId,
        userFrom.id,
        Some(userTo.id),
        NamedEntity(form.id),
        true,
        Answer.Status.Answered,
        isAnonymous = true,
        Set(Answer.Element(form.elements(0).id, Some("text"), None, None))
      )
      when(
        fixture.answerDao.getList(
          optEventId = *,
          optActiveProjectId = eqTo(Some(activeProjectId)),
          optUserFromId = *,
          optFormId = *,
          optUserToId = *,
        )).thenReturn(toFuture(Seq(answer)))
      when(
        fixture.userDao.getList(
          optIds = eqTo(Some(Seq(userFrom.id, userTo.id))),
          optRole = *,
          optStatus = *,
          optGroupIds = *,
          optName = *,
          optEmail = *,
          optProjectIdAuditor = *,
          includeDeleted = eqTo(true),
        )(*)).thenReturn(toFuture(ListWithTotal(Seq(userFrom, userTo))))
      when(fixture.formService.getById(form.id)).thenReturn(toSuccessResult(form))

      val result = wait(fixture.service.getReport(activeProjectId))

      val expectedResult =
        Seq(
          Report(
            Some(userTo),
            Seq(Report.FormReport(
              form,
              Seq(
                Report.FormElementReport(
                  form.elements(0),
                  Seq(Report.FormElementAnswerReport(userFrom, answer.elements.head, isAnonymous = true))),
                Report.FormElementReport(form.elements(1), Seq())
              )
            ))
          ))

      result mustBe expectedResult
    }
  }

  "getAggregatedReport" should {
    "return aggregated report" in {
      val fixture = getFixture
      val elementsWithAnswers = Seq(
        Form.Element(1, TextArea, "", false, Nil, Nil, "") ->
          Answer.Element(1, Some("text"), None, None),
        Form.Element(2, Checkbox, "", false, Nil, Nil, "") ->
          Answer.Element(2, Some("true"), None, None),
        Form.Element(3, Radio, "", false, Seq(Form.ElementValue(10, "radioval")), Nil, "") ->
          Answer.Element(3, None, Some(Set(10)), None)
      )
      val form = Forms(0).copy(id = 1, elements = elementsWithAnswers.map(_._1))
      val userTo = Users(1).copy(id = 2)
      val userFrom = Users(0).copy(id = 3)

      val report =
        Report(
          Some(userTo),
          Seq(
            Report.FormReport(
              form,
              elementsWithAnswers.map {
                case (element, answerElement) =>
                  Report.FormElementReport(element, Seq(Report.FormElementAnswerReport(userFrom, answerElement, false)))
              }
            ))
        )

      val result = fixture.service.getAggregatedReport(report)

      val expectedResult = AggregatedReport(
        Some(userTo),
        Seq(
          AggregatedReport.FormAnswer(
            form,
            Seq(
              AggregatedReport.FormElementAnswer(form.elements(0), "total: 1"),
              AggregatedReport.FormElementAnswer(form.elements(1), """"true" - 1 (100.00%)"""),
              AggregatedReport.FormElementAnswer(form.elements(2), """"radioval" - 1 (100.00%)""")
            )
          ))
      )

      result mustBe expectedResult
    }
  }

  "getAuditorReport" should {
    "return error if user is not auditor" in {
      forAll { (apId: Long) =>
        val fixture = getFixture

        when(
          fixture.userDao.getList(
            optIds = *,
            optRole = *,
            optStatus = *,
            optGroupIds = *,
            optName = *,
            optEmail = *,
            optProjectIdAuditor = eqTo(Some(apId)),
            includeDeleted = *
          )(*)).thenReturn(toFuture(ListWithTotal(Seq.empty[User])))

        val result = wait(fixture.service.getAuditorReport(apId)(UserFixture.user).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[AuthorizationError]
      }
    }

    "return simple report" in {
      val fixture = getFixture

      val activeProjectId = 1
      val form = Forms(0).copy(id = 2)

      val userFrom = Users(0)
      val userTo = Users(1)

      val answer = Answer(
        activeProjectId,
        userFrom.id,
        Some(userTo.id),
        NamedEntity(form.id),
        true,
        Answer.Status.Answered,
        isAnonymous = false,
        Set(Answer.Element(form.elements(0).id, Some("text"), None, None))
      )
      when(
        fixture.userDao.getList(
          optIds = *,
          optRole = *,
          optStatus = *,
          optGroupIds = *,
          optName = *,
          optEmail = *,
          optProjectIdAuditor = eqTo(Some(activeProjectId)),
          includeDeleted = *
        )(*)).thenReturn(toFuture(ListWithTotal(Seq(UserFixture.user))))
      when(
        fixture.answerDao.getList(
          optEventId = *,
          optActiveProjectId = eqTo(Some(activeProjectId)),
          optUserFromId = *,
          optFormId = *,
          optUserToId = *,
        )).thenReturn(toFuture(Seq(answer)))
      when(
        fixture.userDao.getList(
          optIds = eqTo(Some(Seq(userFrom.id, userTo.id))),
          optRole = *,
          optStatus = *,
          optGroupIds = *,
          optName = *,
          optEmail = *,
          optProjectIdAuditor = *,
          includeDeleted = eqTo(true),
        )(*)).thenReturn(toFuture(ListWithTotal(Seq(userFrom, userTo))))
      when(fixture.formService.getById(form.id)).thenReturn(toSuccessResult(form))

      val result = wait(fixture.service.getAuditorReport(activeProjectId)(UserFixture.user).run)

      val expectedResult = Seq(
        SimpleReport(
          userToId = Some(userTo.id),
          detailedReports = Seq(
            SimpleReport.SimpleReportElement(
              userFrom = Some(
                SimpleReport.SimpleReportUser(
                  isAnonymous = false,
                  anonymousId = None,
                  id = Some(userFrom.id))
              ),
              formId = form.id,
              elementId = answer.elements.head.elementId,
              text = "text")),
          aggregatedReports = Seq(
            SimpleReport.SimpleReportElement(
              userFrom = None,
              formId = form.id,
              elementId = answer.elements.head.elementId,
              text = "total: 1"
            ),
            SimpleReport.SimpleReportElement(
              userFrom = None,
              formId = form.id,
              elementId = 2,
              text = ""
            )
          )
        )
      )

      result mustBe 'right
      result.toOption.get mustBe expectedResult
    }
  }
}
