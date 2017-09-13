package services

import javax.inject.{Inject, Singleton}

import models.{ListWithTotal, NamedEntity}
import models.assessment.{Answer, Assessment, PartialAssessment}
import models.dao._
import models.project.ActiveProject
import models.user.User
import services.event.ActiveProjectService
import utils.errors.{ApplicationError, BadRequestError, ConflictError}
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
        Answer(
          activeProjectId,
          account.id,
          assessment.userToId,
          NamedEntity(partial.formId),
          Answer.Status.Answered,
          partial.isAnonymous && project.isAnonymous,
          partial.elements
        )
      }
    }

    def validateAnswer(project: ActiveProject, answer: Answer): EitherT[Future, ApplicationError, Unit] = {
      val result = for {
        existedAnswer <- answerDao.getAnswer(activeProjectId, answer.userFromId, answer.userToId, answer.form.id).lift
        _ <- ensure(existedAnswer.nonEmpty) {
          ConflictError.Assessment.WrongParameters
        }
        _ <- ensure(existedAnswer.get.status == Answer.Status.New || project.canRevote) {
          ConflictError.Assessment.CantRevote
        }
        form <- formService.getById(answer.form.id)
        _ <- answer
          .validateUsing(form)(BadRequestError.Assessment.InvalidForm(_),
                               BadRequestError.Assessment.RequiredAnswersMissed)
          .liftLeft

      } yield ()
      result.leftMap(BadRequestError.Assessment.WithUserFormInfo(_, answer.userToId, answer.form.id))
    }

    for {
      project <- activeProjectService.getById(activeProjectId)

      answers = assessments.flatMap(assessmentToAnswers(project, _))

      _ <- answers.map(validateAnswer(project, _)).collected.leftMap { applicationErrors =>
        BadRequestError.Assessment.Composite(applicationErrors): ApplicationError
      }

      _ <- Future.sequence(answers.map(answerDao.saveAnswer)).lift
    } yield ()

  }
}
