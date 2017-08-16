package services

import javax.inject.{Inject, Singleton}

import models.{ListWithTotal, NamedEntity}
import models.assessment.{Answer, Assessment}
import models.dao._
import models.event.Event
import models.form.Form
import models.project.Relation
import models.user.{User, UserShort}
import play.api.libs.concurrent.Execution.Implicits._
import utils.errors.{ApplicationError, BadRequestError, ConflictError, NotFoundError}
import utils.implicits.FutureLifting._

import scala.concurrent.Future
import scalaz._
import Scalaz._

/**
  * Service for assessment.
  */
@Singleton
class AssessmentService @Inject()(
  protected val formService: FormService,
  protected val userService: UserService,
  protected val groupDao: GroupDao,
  protected val eventDao: EventDao,
  protected val relationDao: ProjectRelationDao,
  protected val answerDao: AnswerDao,
  protected val projectDao: ProjectDao
) extends ServiceResults[Assessment] {

  /**
    * Returns list of assessment objects available to proccess.
    *
    * @param eventId   ID of event
    * @param projectId Id of project
    */
  def getList(eventId: Long, projectId: Long)(implicit account: User): ListResult = {

    /**
      * Returns forms paired with answers.
      *
      * @param forms IDs of the forms
      * @param user    assessed user, none for survey forms
      */
    def getAnswersForForms(forms: Seq[NamedEntity],
                           user: Option[User] = None): Future[Seq[(NamedEntity, Option[Answer.Form])]] = {
      Future.sequence {
        forms.distinct
          .map { form =>
            answerDao
              .getAnswer(eventId, projectId, account.id, user.map(_.id), form.id)
              .map((form, _))
          }
      }
    }

    /**
      * Maps survey relations to assessment object.
      */
    def surveyRelationsToAssessments(relations: Seq[Relation]): Future[Option[Assessment]] = {
      val forms =
        relations
          .filter(_.kind == Relation.Kind.Survey)
          .map(_.form)

      getAnswersForForms(forms).map { formsWithAnswers =>
        if (formsWithAnswers.isEmpty) None
        else Some(Assessment(formsWithAnswers))
      }
    }

    /**
      * Maps classic relations to assessment objects.
      */
    def classicRelationsToAssessments(relations: Seq[Relation]): Future[Seq[Assessment]] = {

      /**
        * Returns assessed users for relation.
        */
      def getUsersForRelation(relation: Relation): Future[Seq[User]] = {
        userService
          .listByGroupId(
            relation.groupTo.getOrElse(throw new NoSuchElementException("group to is not defined")).id,
            includeDeleted = false
          )
          .map(_.data)
          .run
          .map(_.getOrElse(Nil))
      }

      /**
        * Maps relation-users tuple to assessments objects.
        */
      def userWithRelationToAssessments(usersWithRelation: Seq[(Seq[User], Relation)]): Future[Seq[Assessment]] = {
        Future.sequence {
          usersWithRelation
            .flatMap {
              case (users, relation) =>
                users.map((_, relation))
            }
            .filter { case (user, relation) => user.id != account.id || relation.canSelfVote }
            .groupBy { case (user, _) => user }
            .map {
              case (user, relationsWithUsers) =>
                val forms = relationsWithUsers.map { case (_, relation) => relation.form }.distinct

                getAnswersForForms(forms, Some(user)).map { formsWithAnswers =>
                  Assessment(formsWithAnswers, Some(user))
                }
            }
            .toSeq
        }
      }

      val usersWithRelation = relations
        .filter(x => x.kind == Relation.Kind.Classic && x.groupTo.isDefined)
        .map { relation =>
          getUsersForRelation(relation).map((_, relation))
        }

      Future.sequence(usersWithRelation).flatMap(userWithRelationToAssessments)
    }

    /**
      * Replace form templates ids with freezed ids forms in relations.
      */
    def replaceTemplatesWithFreezedForms(relations: Seq[Relation]) = {
      val formIds = relations.map(_.form.id).distinct
      val templateIdTofreezedForm: Future[Map[Long, Form]] = Future
        .sequence {
          formIds.map { formId =>
            formService
              .getOrCreateFreezedForm(eventId, formId)
              .run
              .map(x => (formId, x.getOrElse(throw new NoSuchElementException("Missed freezed form"))))
          }
        }
        .map(_.toMap)

      templateIdTofreezedForm.map { mapping =>
        relations.map { relation =>
          val freezedForm = mapping(relation.form.id)
          relation.copy(form = NamedEntity(freezedForm.id, freezedForm.name))
        }
      }
    }

    for {
      userGroups <- groupDao.findGroupIdsByUserId(account.id).lift

      events <- eventDao
        .getList(
          optId = Some(eventId),
          optProjectId = Some(projectId),
          optGroupFromIds = Some(userGroups)
        )
        .lift
      _ <- ensure(events.total == 1) {
        NotFoundError.Assessment(eventId, projectId, account.id)
      }

      relations <- relationDao.getList(optProjectId = Some(projectId)).lift

      userRelations <- replaceTemplatesWithFreezedForms(
        relations.data.filter(x => userGroups.contains(x.groupFrom.id))).lift

      surveyAssessment <- surveyRelationsToAssessments(userRelations).lift
      classicAssessments <- classicRelationsToAssessments(userRelations).lift
      assessments = surveyAssessment.toSeq ++ classicAssessments
    } yield ListWithTotal(assessments.length, assessments)
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
  def bulkSubmit(eventId: Long, projectId: Long, assessments: Seq[Assessment])(implicit account: User): UnitResult = {
    type UserAnswer = (Option[Long], Answer.Form, Boolean, String, String)

    def validateAssessment(assessment: Assessment): Future[Validation[ApplicationError, UserAnswer]] = {
      val invalidForm = BadRequestError.Assessment.InvalidForm(_)
      val userToId = assessment.user.map(_.id)
      val answer = assessment.forms.headOption.getOrElse(throw new NoSuchElementException("missed form in assessment"))

      /**
        * Checks ability of user to submit assessment with given user and form IDs.
        */
      def validateAssessment(userGroups: Seq[Long]): EitherT[Future, ApplicationError, Unit] = {

        def validateRelation(relation: Relation): Future[Boolean] = {
          val groupFromIsOk = userGroups.contains(relation.groupFrom.id)
          val selfVotingIsOk = !userToId.contains(account.id) || relation.canSelfVote
          val eitherT = for {
            groupToIsOk <- (relation.groupTo, userToId) match {
              case (Some(groupTo), Some(userId)) =>
                userService.listByGroupId(groupTo.id, includeDeleted = false).map(_.data.exists(_.id == userId))
              case (None, None) => true.lift
              case _ => false.lift
            }
            freezedForm <- formService.getOrCreateFreezedForm(eventId, relation.form.id)
          } yield groupFromIsOk && groupToIsOk && freezedForm.id == answer.form.id && selfVotingIsOk
          eitherT.getOrElse(false)
        }

        for {
          relations <- relationDao
            .getList(
              optProjectId = Some(projectId),
              optKind = Some(if (userToId.isDefined) Relation.Kind.Classic else Relation.Kind.Survey)
            )
            .lift
          validationResults <- Future.sequence(relations.data.map(validateRelation)).lift
          _ <- ensure(validationResults.contains(true)) {
            ConflictError.Assessment.WrongParameters
          }
        } yield ()
      }

      val result = for {
        userGroups <- groupDao.findGroupIdsByUserId(account.id).lift

        events <- eventDao
          .getList(
            optId = Some(eventId),
            optProjectId = Some(projectId),
            optGroupFromIds = Some(userGroups),
            optStatus = Some(Event.Status.InProgress)
          )
          .lift
        _ <- ensure(events.total == 1) {
          NotFoundError.Assessment(eventId, projectId, account.id)
        }
        event = events.data.head

        project <- projectDao.findById(projectId).lift

        _ <- ensure(project.nonEmpty) {
          NotFoundError.Assessment(eventId, projectId, account.id)
        }

        existedAnswer <- answerDao.getAnswer(eventId, projectId, account.id, userToId, answer.form.id).lift
        _ <- ensure(existedAnswer.isEmpty || project.get.canRevote) {
          ConflictError.Assessment.CantRevote
        }

        form <- formService.getById(answer.form.id)
        _ <- answer.validateUsing(form)(invalidForm, BadRequestError.Assessment.RequiredAnswersMissed).liftLeft

        _ <- if (existedAnswer.isEmpty) validateAssessment(userGroups) else ().lift
      } yield
        (
          userToId,
          answer,
          project.getOrElse(throw new NoSuchElementException("Project not found")).isAnonymous,
          project.getOrElse(throw new NoSuchElementException("Project not found")).machineName,
          form.machineName
        )

      result.fold(
        error => Failure(BadRequestError.Assessment.WithUserFormInfo(error, userToId, answer.form.id)),
        Success(_)
      )
    }

    val validationResult = Future
      .sequence {
        assessments.map(validateAssessment)
      }
      .map { validateAssessments =>
        validateAssessments.foldLeft(Seq.empty[UserAnswer].success[Seq[ApplicationError]]) {
          case (Failure(acc), Failure(current)) => Failure(acc :+ current)
          case (acc @ Failure(_), _) => acc
          case (Success(acc), Success(el)) => Success(acc :+ el)
          case (_, Failure(error)) => Failure(Seq(error))
        }
      }

    EitherT.eitherT {
      validationResult.flatMap {
        case Success(answers) =>
          Future
            .sequence {
              answers.map {
                case (userToId, answer, isAnonymous, projectMachineName, formMachineName) =>
                  answerDao.saveAnswer(
                    eventId,
                    projectId,
                    account.id,
                    userToId,
                    answer.copy(isAnonymous = isAnonymous && answer.isAnonymous),
                    projectMachineName,
                    formMachineName
                  )
              }
            }
            .map(_ => ().right)
        case Failure(errors) => BadRequestError.Assessment.Composite(errors).left.toFuture
      }
    }
  }
}
