package services

import javax.inject.{Inject, Singleton}

import models.{ListWithTotal, NamedEntity}
import models.assessment.{Answer, Assessment, PartialAssessment}
import models.dao._
import models.event.Event
import models.project.ActiveProject
import models.user.User
import services.event.ActiveProjectService
import utils.errors.{ApplicationError, BadRequestError, ConflictError, NotFoundError}
import utils.implicits.FutureLifting._
import utils.implicits.RichEitherT._

import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import Scalaz._

/**
  * Service for assessment.
  */
@Singleton
class AssessmentService @Inject()(
  protected val formService: FormService,
  protected val userDao: UserDao,
  protected val answerDao: AnswerDao,
  protected val activeProjectService: ActiveProjectService,
  protected val eventDao: EventDao,
  implicit val ec: ExecutionContext
) extends ServiceResults[Assessment] {

  /**
    * Returns list of assessment objects available to proccess.
    */
  def getList(activeProjectId: Long)(implicit account: User): ListResult = {

    def getAssessments(answers: Seq[Answer]) = {
      val grouped = answers.groupBy(_.userToId)
      val usersIds = grouped.keys.toSeq.collect { case Some(v) => v }.distinct
      for {
        users <- userDao.getList(optIds = Some(usersIds), includeDeleted = true)
      } yield {
        grouped.map {
          case (userToId, answers) =>
            val user = userToId.flatMap(uid => users.data.find(_.id == uid))
            Assessment(user, answers)
        }.toSeq
      }
    }

    for {
      activeProject <- activeProjectService.getById(activeProjectId)
      answers <- answerDao.getList(optActiveProjectId = Some(activeProjectId), optUserFromId = Some(account.id)).lift
      assessments <- getAssessments(answers).lift
    } yield ListWithTotal(assessments)
  }

  /**
    * Bulk submits assessment answers.
    *
    * @param eventId     event ID
    * @param projectId   project ID
    * @param assessments assessments models
    * @param account     logged in user
    * @return none in success case, error otherwise
    */
  def bulkSubmit(activeProjectId: Long, assessments: Seq[PartialAssessment])(implicit account: User) = {

    def assessmentToAnswers(project: ActiveProject, assessment: PartialAssessment) = {
      assessment.answers.map { partial =>
        val (status, elements) =
          if (partial.isSkipped) {
            (Answer.Status.Skipped, Set.empty[Answer.Element])
          } else {
            (Answer.Status.Answered, partial.elements)
          }
        Answer(
          activeProjectId,
          account.id,
          assessment.userToId,
          NamedEntity(partial.formId),
          true,
          status,
          partial.isAnonymous && project.isAnonymous,
          elements
        )
      }
    }

    def validateAnswer(project: ActiveProject, answer: Answer): EitherT[Future, ApplicationError, Answer] = {
      val result = for {
        maybeExistedAnswer <- answerDao
          .getAnswer(activeProjectId, answer.userFromId, answer.userToId, answer.form.id)
          .lift
        _ <- ensure(maybeExistedAnswer.nonEmpty) {
          ConflictError.Assessment.WrongParameters
        }
        existedAnswer = maybeExistedAnswer.get
        _ <- ensure(existedAnswer.status != Answer.Status.Answered || project.canRevote) {
          ConflictError.Assessment.CantRevote
        }
        _ <- ensure(answer.status != Answer.Status.Skipped || existedAnswer.canSkip) {
          ConflictError.Assessment.CantSkip
        }
        form <- formService.getById(answer.form.id)
        _ <- answer
          .validateUsing(form)(BadRequestError.Assessment.InvalidForm(_),
                               BadRequestError.Assessment.RequiredAnswersMissed)
          .liftLeft

      } yield answer.copy(canSkip = existedAnswer.canSkip)
      result.leftMap(BadRequestError.Assessment.WithUserFormInfo(_, answer.userToId, answer.form.id))
    }

    for {
      project <- activeProjectService.getById(activeProjectId)
      event <- eventDao.findById(project.eventId).liftRight(NotFoundError.Event(project.eventId))
      _ <- ensure(event.status == Event.Status.InProgress) {
        ConflictError.Assessment.InactiveEvent
      }

      answers = assessments.flatMap(assessmentToAnswers(project, _))

      validatedAnswers <- answers.map(validateAnswer(project, _)).collected.leftMap { applicationErrors =>
        BadRequestError.Assessment.Composite(applicationErrors): ApplicationError
      }

      _ <- Future.sequence(validatedAnswers.map(answerDao.saveAnswer)).lift
    } yield ()

  }
}
