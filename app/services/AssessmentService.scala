package services

import javax.inject.{Inject, Singleton}

import models.{ListWithTotal, NamedEntity}
import models.assessment.{Answer, Assessment}
import models.dao.{AnswerDao, EventDao, GroupDao, ProjectRelationDao}
import models.event.Event
import models.form.Form
import models.project.Relation
import models.user.{User, UserShort}
import play.api.libs.concurrent.Execution.Implicits._
import utils.errors.{ApplicationError, BadRequestError, ConflictError, NotFoundError}
import utils.implicits.FutureLifting._

import scala.concurrent.Future
import scalaz._


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
  protected val answerDao: AnswerDao
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
    def getAnswersForForms(forms: Seq[NamedEntity], user: Option[User] = None): Future[Seq[(NamedEntity, Option[Answer.Form])]] = {
      Future.sequence {
        forms
          .distinct
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
          .listByGroupId(relation.groupTo.getOrElse(throw new NoSuchElementException("group to not defined")).id)
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
            .flatMap { case (users, relation) =>
              users.map((_, relation))
            }
            .groupBy { case (user, _) => user }
            .map { case (user, relationsWithUsers) =>
              val forms = relationsWithUsers
                .map { case (_, relation) => relation.form }
                .distinct

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
      val templateIdTofreezedForm: Future[Map[Long, Form]] = Future.sequence {
        formIds.map { formId =>
          formService
            .getOrCreateFreezedForm(eventId, formId)
            .run
            .map(x => (formId, x.getOrElse(throw new NoSuchElementException("Missed freezed form"))))
        }
      }.map(_.toMap)

      templateIdTofreezedForm.map { mapping =>
        relations.map { relation =>
          val freezedForm = mapping(relation.form.id)
          relation.copy(form = NamedEntity(freezedForm.id, freezedForm.name))
        }
      }
    }

    for {
      userGroups <- groupDao.findGroupIdsByUserId(account.id).lift

      events <- eventDao.getList(
        optId = Some(eventId),
        optProjectId = Some(projectId),
        optGroupFromIds = Some(userGroups)
      ).lift
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
    * Submits assessment answer.
    *
    * @param eventId    event ID
    * @param projectId  project ID
    * @param assessment assessment model
    * @param account    logged in user
    * @return none in success case, error otherwise
    */
  def submit(eventId: Long, projectId: Long, assessment: Assessment)(implicit account: User): UnitResult = {

    val invalidForm = BadRequestError.Assessment.InvalidForm(_)
    val userToId = assessment.user.map(_.id)
    val answer = assessment.forms.headOption.getOrElse(throw new NoSuchElementException("missed form in assessment"))

    /**
      * Validates form. Returns none in case of success.
      */
    def validateForm(form: Form): Option[ApplicationError] = {

      def validateElementAnswer(answerElement: Answer.Element): Option[ApplicationError] = {
        form.elements.find(_.id == answerElement.elementId) match {
          case Some(formElement) =>
            lazy val answerIsText = answerElement.text.isDefined
            lazy val answerIsValues = answerElement.valuesIds.isDefined
            lazy val needValues = formElement.kind.needValues
            lazy val answerValuesMatchFormValues =
              answerElement.valuesIds.getOrElse(Nil).toSet.subsetOf(formElement.values.map(_.id).toSet)

            if (needValues && answerIsText) Some(invalidForm("Values element contains text answer"))
            else if (!needValues && answerIsValues) Some(invalidForm("Text element contains values answer"))
            else if (needValues && !answerValuesMatchFormValues) Some(invalidForm("Values answer contains unknown valueId"))
            else if (!needValues && !answerIsText) Some(invalidForm("Text answer is missed"))
            else None
          case None => Some(invalidForm("Unknown answer elementId"))
        }
      }

      lazy val answerElementsIds = answer.answers.map(_.elementId)
      lazy val elementAnswersAreDistinct = answerElementsIds.size == answer.answers.size
      lazy val allRequiredElementsAreAnswered = form.elements.filter(_.required).forall(x => answerElementsIds.contains(x.id))
      lazy val maybeElementValidationError = answer.answers.toSeq.map(validateElementAnswer).fold(None) {
        case (err@Some(_), _) => err
        case (None, maybeError) => maybeError
      }

      if (!elementAnswersAreDistinct) Some(invalidForm("Duplicate elementId in answers"))
      else if (!allRequiredElementsAreAnswered) Some(BadRequestError.Assessment.RequiredAnswersMissed)
      else if (maybeElementValidationError.nonEmpty) maybeElementValidationError
      else None
    }

    /**
      * Checks ability of user to submit assessment with given user and form IDs.
      */
    def validateAssessment(userGroups: Seq[Long]): EitherT[Future, ApplicationError, Unit] = {

      def validateRelation(relation: Relation): Future[Boolean] = {
        val groupFromIsOk = userGroups.contains(relation.groupFrom.id)
        val eitherT = for {
          groupToIsOk <- (relation.groupTo, userToId) match {
            case (Some(groupTo), Some(userId)) =>
              userService.listByGroupId(groupTo.id).map(_.data.exists(_.id == userId))
            case (None, None) => true.lift
            case _ => false.lift
          }
          freezedForm <- formService.getOrCreateFreezedForm(eventId, relation.form.id)
        } yield groupFromIsOk && groupToIsOk && freezedForm.id == answer.form.id
        eitherT.getOrElse(false)
      }

      for {
        relations <- relationDao.getList(
          optProjectId = Some(projectId),
          optKind = Some(if (userToId.isDefined) Relation.Kind.Classic else Relation.Kind.Survey)
        ).lift
        validationResults <- Future.sequence(relations.data.map(validateRelation)).lift
        _ <- ensure(validationResults.contains(true)) {
          ConflictError.Assessment.WrongParameters
        }
      } yield ()
    }

    for {
      userGroups <- groupDao.findGroupIdsByUserId(account.id).lift

      events <- eventDao.getList(
        optId = Some(eventId),
        optProjectId = Some(projectId),
        optGroupFromIds = Some(userGroups),
        optStatus = Some(Event.Status.InProgress)
      ).lift
      _ <- ensure(events.total == 1) {
        NotFoundError.Assessment(eventId, projectId, account.id)
      }
      event = events.data.head

      existedAnswer <- answerDao.getAnswer(eventId, projectId, account.id, userToId, answer.form.id).lift
      _ <- ensure(existedAnswer.isEmpty || event.canRevote) {
        ConflictError.Assessment.CantRevote
      }

      form <- formService.getById(answer.form.id)
      _ <- validateForm(form).liftLeft

      _ <- if (existedAnswer.isEmpty) validateAssessment(userGroups) else ().lift

      _ <- answerDao.saveAnswer(eventId, projectId, account.id, userToId, answer).lift
    } yield ()
  }
}
