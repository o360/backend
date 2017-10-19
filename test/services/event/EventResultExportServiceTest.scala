package services.event

import models.ListWithTotal
import models.assessment.Answer
import models.competence.{Competence, CompetenceGroup}
import models.dao._
import models.form.Form
import models.user.User
import services.BaseServiceTest
import org.mockito.Mockito._
import testutils.generator._

import scala.collection.JavaConverters._

/**
  * Test for event result export service.
  */
class EventResultExportServiceTest
  extends BaseServiceTest
  with AnswerGenerator
  with FormGenerator
  with UserGenerator
  with CompetenceGenerator
  with CompetenceGroupGenerator {

  private case class Fixture(
    answerDao: AnswerDao,
    formDao: FormDao,
    userDao: UserDao,
    eventDao: EventDao,
    competenceDao: CompetenceDao,
    competenceGroupDao: CompetenceGroupDao,
    service: EventResultExportService
  )

  private def getFixture = {
    val answerDao = mock[AnswerDao]
    val formDao = mock[FormDao]
    val userDao = mock[UserDao]
    val eventDao = mock[EventDao]
    val competenceDao = mock[CompetenceDao]
    val competenceGroupDao = mock[CompetenceGroupDao]
    val service = new EventResultExportService(answerDao, formDao, userDao, eventDao, competenceDao, competenceGroupDao, ec)
    Fixture(answerDao, formDao, userDao, eventDao, competenceDao, competenceGroupDao, service)
  }

  "export answers" should {
    "export answers" in {
      forAll {
        (
        eventId: Long,
        answers: Seq[Answer],
        form: Form,
        users: Seq[User],
        competencies: Seq[Competence],
        competenceGroups: Seq[CompetenceGroup]
        ) =>
          val fixture = getFixture

          when(fixture.answerDao.getList(
            optEventId = eqTo(Some(eventId)),
            optActiveProjectId = *,
            optUserFromId = *,
            optFormId = *,
            optUserToId = *,
          )).thenReturn(toFuture(answers))
          val formIdCaptor = argumentCaptor[Long]
          when(fixture.formDao.findById(formIdCaptor.capture())).thenReturn(toFuture(Some(form)))

          val userIdsCaptor = argumentCaptor[Option[Seq[Long]]]
          when(fixture.userDao.getList(
            optIds = userIdsCaptor.capture(),
            optRole = *,
            optStatus = *,
            optGroupIds = *,
            optName = *,
            optEmail = *,
            optProjectIdAuditor = *,
            eqTo(true)
          )(*)).thenReturn(toFuture(ListWithTotal(users)))

          val competenceIdsCaptor = argumentCaptor[Option[Seq[Long]]]
          when(fixture.competenceDao.getList(
            optGroupId = *,
            optKind = *,
            optIds = competenceIdsCaptor.capture(),
          )(*)).thenReturn(toFuture(ListWithTotal(competencies)))

          val competenceGroupIdsCaptor = argumentCaptor[Option[Seq[Long]]]
          when(fixture.competenceGroupDao.getList(
            optKind = *,
            optIds = competenceGroupIdsCaptor.capture()
          )(*)).thenReturn(toFuture(ListWithTotal(competenceGroups)))

          val (_, resultUsers, resultAnswers, resultCompetencies, resultCompetenceGroups) =
            wait(fixture.service.exportAnswers(eventId))

          formIdCaptor.getAllValues.asScala must contain theSameElementsAs answers.map(_.form.id).distinct

          userIdsCaptor.getValue mustBe defined
          userIdsCaptor.getValue.get must contain allElementsOf answers.filterNot(_.isAnonymous).map(_.userFromId)
          userIdsCaptor.getValue.get must contain allElementsOf answers.flatMap(_.userToId)

          competenceIdsCaptor.getValue mustBe defined
          competenceIdsCaptor.getValue.get must contain theSameElementsAs form.elements.flatMap(_.competencies).map(_.competence.id).distinct

          competenceGroupIdsCaptor.getValue mustBe defined
          competenceGroupIdsCaptor.getValue.get must contain theSameElementsAs competencies.map(_.groupId).distinct

          users must contain allElementsOf resultUsers
          resultAnswers must have length answers.length
          resultCompetencies mustBe competencies
          resultCompetenceGroups mustBe competenceGroups
      }
    }
  }
}
