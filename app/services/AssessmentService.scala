package services

import javax.inject.{Inject, Singleton}

import models.ListWithTotal
import models.assessment.Assessment
import models.dao.{EventDao, GroupDao, ProjectRelationDao}
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
  protected val relationDao: ProjectRelationDao
) extends ServiceResults[Assessment] {

  /**
    * Returns list of assessment objects available to proccess.
    *
    * @param eventId   ID of event
    * @param projectId Id of project
    */
  def getList(eventId: Long, projectId: Long)(implicit account: User): ListResult = {

    /**
      * Maps survey relations to assessment objects.
      */
    def surveyRelationsToAssessments(relations: Seq[Relation]) = {
      val formIds = relations
        .filter(_.kind == Relation.Kind.Survey)
        .map(_.form.id)
        .distinct

      if (formIds.nonEmpty) Seq(Assessment(None, formIds))
      else Nil
    }

    /**
      * Maps classic relations to assessment objects.
      */
    def classicRelationsToAssessments(relations: Seq[Relation]) = {

      /**
        * Returns assessed users for relation.
        */
      def getUsersForRelation(relation: Relation) = {
        userService
          .listByGroupId(relation.groupTo.get.id)
          .map(_.data)
          .run
          .map(_.getOrElse(Nil))
      }

      /**
        * Maps relation-users tuple to assessments objects.
        */
      def userWithRelationToAssessments(usersWithRelation: Seq[(Seq[User], Relation)]) = {
        usersWithRelation
          .flatMap { case (users, relation) =>
            users.map((_, relation))
          }
          .groupBy { case (user, _) => user }
          .map { case (user, relationsWithUsers) =>
            val formIds = relationsWithUsers
              .map { case (_, relation) => relation.form.id }
              .distinct

            Assessment(Some(UserShort.fromUser(user)), formIds)
          }
          .toSeq
      }

      val usersWithRelation = relations
        .filter(x => x.kind == Relation.Kind.Classic && x.groupTo.isDefined)
        .map { relation =>
          getUsersForRelation(relation).map((_, relation))
        }

      Future.sequence(usersWithRelation).map(userWithRelationToAssessments)
    }

    /**
      * Replace form templates with freezed forms in assessments.
      */
    def replaceTemplatesWithFreezedForms(assessments: Seq[Assessment]) = {
      val formIds = assessments.flatMap(_.formIds).distinct
      val templateIdTofreezedForm = Future.sequence {
        formIds.map { formId =>
          formService
            .getOrCreateFreezedForm(eventId, formId)
            .run
            .map(x => (formId, x.getOrElse(throw new NoSuchElementException("Missed freezed form"))))
        }
      }.map(_.toMap)

      templateIdTofreezedForm.map { mapping =>
        assessments.map { assessment =>
          val freezedFormIds = assessment.formIds.map(mapping(_).id)
          assessment.copy(formIds = freezedFormIds)
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

      userRelations = relations.data.filter(x => userGroups.contains(x.groupFrom.id))

      surveyAssessment = surveyRelationsToAssessments(userRelations)
      classicAssessments <- classicRelationsToAssessments(userRelations).lift
      assessments <- replaceTemplatesWithFreezedForms(surveyAssessment ++ classicAssessments).lift
    } yield ListWithTotal(assessments.length, assessments)
  }
}
