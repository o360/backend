package services

import javax.inject.{Inject, Singleton}

import models.{ListWithTotal, NamedEntity}
import models.assessment.{Answer, Assessment}
import models.dao.{AnswerDao, EventDao, GroupDao, ProjectRelationDao}
import models.form.Form
import models.project.Relation
import models.user.{User, UserShort}
import play.api.libs.concurrent.Execution.Implicits._
import utils.errors.NotFoundError
import utils.implicits.FutureLifting._

import scala.concurrent.Future


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
      * Returns formIds paired with answers.
      *
      * @param formIds IDs of the forms
      * @param user    assessed user, none for survey forms
      */
    def getAnswersForForms(formIds: Seq[Long], user: Option[User] = None): Future[Seq[(Long, Option[Answer.Form])]] = {
      Future.sequence {
        formIds
          .distinct
          .map { formId =>
            answerDao.getAnswer(eventId, projectId, account.id, user.map(_.id), formId).map((formId, _))
          }
      }
    }

    /**
      * Maps survey relations to assessment objects.
      */
    def surveyRelationsToAssessments(relations: Seq[Relation]): Future[Seq[Assessment]] = {
      val formIds =
        relations
        .filter(_.kind == Relation.Kind.Survey)
        .map(_.form.id)

      val formsWithAnswersF = getAnswersForForms(formIds)

      formsWithAnswersF.map { formsWithAnswers =>
        if (formsWithAnswers.isEmpty) Nil
        else Seq(Assessment(formsWithAnswers))
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
          .listByGroupId(relation.groupTo.get.id)
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
              val formIds = relationsWithUsers
                .map { case (_, relation) => relation.form.id }
                .distinct

              val formsWithAnswersF = getAnswersForForms(formIds, Some(user))

              formsWithAnswersF.map { formsWithAnswers =>
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
      assessments = surveyAssessment ++ classicAssessments
    } yield ListWithTotal(assessments.length, assessments)
  }
}
