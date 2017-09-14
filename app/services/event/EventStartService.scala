package services.event

import javax.inject.{Inject, Singleton}

import models.NamedEntity
import models.assessment.Answer
import models.dao.{AnswerDao, ProjectDao, ProjectRelationDao}
import models.event.EventJob
import models.form.Form
import models.project.{ActiveProject, Project, Relation}
import models.user.User
import services._
import utils.errors.ApplicationError

import scalaz._
import Scalaz._
import utils.implicits.RichEitherT._
import utils.implicits.FutureLifting._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Event start handling service.
  */
@Singleton
class EventStartService @Inject()(
  formService: FormService,
  projectDao: ProjectDao,
  relationDao: ProjectRelationDao,
  activeProjectService: ActiveProjectService,
  userService: UserService,
  answerDao: AnswerDao,
  implicit val ec: ExecutionContext
) extends ServiceResults[Unit] {

  def execute(job: EventJob.EventStart): Future[Unit] = {

    def getAnswersForRelation(relation: Relation,
                              activeProject: ActiveProject): EitherT[Future, ApplicationError, Seq[Answer]] = {
      for {
        usersFrom <- userService.listByGroupId(relation.groupFrom.id, includeDeleted = false)
        usersTo <- relation.groupTo match {
          case Some(groupTo) =>
            userService
              .listByGroupId(groupTo.id, includeDeleted = false)
              .map(_.data.some)
          case None =>
            val result: EitherT[Future, ApplicationError, Option[Seq[User]]] = EitherT.right(none[Seq[User]].toFuture)
            result
        }
      } yield {
        usersFrom.data.flatMap { userFrom =>
          val createAnswer =
            Answer(activeProject.id, userFrom.id, _: Option[Long], relation.form, relation.canSkipAnswers)
          usersTo match {
            case Some(users) =>
              users
                .filter(u => u.id != userFrom.id || relation.canSelfVote)
                .map(u => createAnswer(Some(u.id)))
            case None => Seq(createAnswer(None))
          }
        }
      }
    }

    def getAnswersForProject(project: Project) = {

      for {
        activeProject <- activeProjectService.create(project, job.eventId)

        auditors <- userService.listByGroupId(project.groupAuditor.id, includeDeleted = false)
        _ <- activeProjectService.createProjectAuditors(activeProject.id, auditors.data.map(_.id))

        relations <- relationDao.getList(optProjectId = Some(project.id)).lift

        allAnswers <- relations.data.map(getAnswersForRelation(_, activeProject)).sequenced
        answers = allAnswers.flatten.distinct
      } yield answers
    }

    def createAndReplaceForms(answers: Seq[Answer]) = {
      val formsIds = answers.map(_.form.id).distinct
      for {
        forms <- formsIds.map(formService.getById).sequenced
        mapping <- forms
          .map(f => formService.create(f.copy(kind = Form.Kind.Freezed)).map(c => (f.id, c.id)))
          .sequenced
          .map(_.toMap)
      } yield answers.map(a => a.copy(form = NamedEntity(mapping(a.form.id))))
    }

    val result = for {
      projects <- projectDao.getList(optEventId = Some(job.eventId)).lift
      allAnswers <- projects.data.map(getAnswersForProject).sequenced
      withReplacedForms <- createAndReplaceForms(allAnswers.flatten)
      _ <- Future.sequence(withReplacedForms.map(answerDao.saveAnswer)).lift
    } yield ()

    result.leftMap[Unit](e => throw new RuntimeException(e.getMessage)).run.map(_ => ())
  }
}
