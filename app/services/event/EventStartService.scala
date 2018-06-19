package services.event

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import models.{EntityKind, NamedEntity}
import models.assessment.Answer
import models.dao._
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
  eventDao: EventDao,
  formService: FormService,
  projectDao: ProjectDao,
  relationDao: ProjectRelationDao,
  activeProjectService: ActiveProjectService,
  userService: UserService,
  answerDao: AnswerDao,
  competenceDao: CompetenceDao,
  competenceGroupDao: CompetenceGroupDao,
  system: ActorSystem
) extends ServiceResults[Unit] {

  implicit val ec: ExecutionContext = system.dispatchers.lookup("contexts.db-write")

  private[event] def getAnswersForRelation(
    relation: Relation,
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

  private[event] def getAnswersForProject(
    eventId: Long,
    project: Project,
    relationAnswersProducer: (Relation, ActiveProject) => EitherT[Future, ApplicationError, Seq[Answer]]) = {

    for {
      activeProject <- activeProjectService.create(project, eventId)

      auditors <- userService.listByGroupId(project.groupAuditor.id, includeDeleted = false)
      _ <- activeProjectService.createProjectAuditors(activeProject.id, auditors.data.map(_.id))

      relations <- relationDao.getList(optProjectId = Some(project.id)).lift

      allAnswers <- relations.data.map(relationAnswersProducer(_, activeProject)).sequenced
      answers = allAnswers.flatten.distinct
    } yield answers
  }

  private[event] def createAndReplaceCompetencies(forms: Seq[Form]) = {
    val competenciesIds = forms.flatMap(_.elements.flatMap(_.competencies)).map(_.competence.id).distinct

    for {
      competencies <- competenceDao.getList(optIds = Some(competenciesIds))

      groupsIds = competencies.data.map(_.groupId).distinct
      groups <- competenceGroupDao.getList(optIds = Some(groupsIds))

      groupsMapping <- Future
        .sequence {
          groups.data.map(cg =>
            competenceGroupDao.create(cg.copy(kind = EntityKind.Freezed)).map(created => (cg.id, created.id)))
        }
        .map(_.toMap)

      mapping <- Future
        .sequence {
          competencies.data
            .filter(c => groupsMapping.contains(c.groupId))
            .map { competence =>
              val withReplacedGroup =
                competence.copy(groupId = groupsMapping(competence.groupId), kind = EntityKind.Freezed)
              competenceDao.create(withReplacedGroup).map(created => (competence.id, created.id))
            }
        }
        .map(_.toMap)
    } yield {
      forms.map { form =>
        val elements = form.elements.map { element =>
          val competencies = element.competencies.flatMap { elementCompetence =>
            mapping
              .get(elementCompetence.competence.id)
              .map { newCompetenceId =>
                elementCompetence.copy(competence = NamedEntity(newCompetenceId))
              }
              .toSeq
          }
          element.copy(competencies = competencies)
        }
        form.copy(elements = elements)
      }
    }
  }

  private[event] def createAndReplaceForms(answers: Seq[Answer], replaceCompetencies: Seq[Form] => Future[Seq[Form]]) = {
    val formsIds = answers.map(_.form.id).distinct
    for {
      forms <- formsIds.map(formService.getById).sequenced
      withCompetencies <- replaceCompetencies(forms).lift
      mapping <- withCompetencies
        .map(f => formService.create(f.copy(kind = Form.Kind.Freezed)).map(c => (f.id, c.id)))
        .sequenced
        .map(_.toMap)
    } yield answers.map(a => a.copy(form = NamedEntity(mapping(a.form.id))))
  }

  private[event] def distinctAnswers(answers: Seq[Answer]): Seq[Answer] =
    answers
      .groupBy(_.getUniqueComponent)
      .map {
        case (_, conflictedAnswers) =>
          val canSkip = conflictedAnswers.map(_.canSkip).fold(false)(_ || _)
          conflictedAnswers.head.copy(canSkip = canSkip)
      }
      .toSeq

  def execute(job: EventJob.EventStart): Future[Unit] = {

    val result = for {
      _ <- eventDao.setIsPreparing(job.eventId, isPreparing = true).lift
      projects <- projectDao.getList(optEventId = Some(job.eventId)).lift
      allAnswers <- projects.data.map(getAnswersForProject(job.eventId, _, getAnswersForRelation)).sequenced
      withoutDuplicates = distinctAnswers(allAnswers.flatten)
      withReplacedForms <- createAndReplaceForms(withoutDuplicates, createAndReplaceCompetencies)
      _ <- answerDao.createAnswers(withReplacedForms).lift
      _ <- eventDao.setIsPreparing(job.eventId, isPreparing = false).lift
    } yield ()

    result.leftMap[Unit](e => throw new RuntimeException(e.getMessage)).run.map(_ => ())
  }
}
