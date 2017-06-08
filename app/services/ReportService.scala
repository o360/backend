package services

import javax.inject.{Inject, Singleton}

import models.assessment.Answer
import models.dao.{AnswerDao, ProjectRelationDao}
import models.form.Form
import models.project.Relation
import models.report._
import models.user.User
import play.api.libs.concurrent.Execution.Implicits._
import services.ReportService.Combination
import utils.Logger
import utils.implicits.FutureLifting._

import scala.concurrent.Future


/**
  * Report service.
  */
@Singleton
class ReportService @Inject()(
  userService: UserService,
  relationDao: ProjectRelationDao,
  formService: FormService,
  answerDao: AnswerDao
) extends Logger {

  /**
    * Returns reports for all assessed users in given event and project.
    *
    * @param eventId   ID of event
    * @param projectId ID of project
    */
  def getReport(eventId: Long, projectId: Long): Future[Seq[Report]] = {
    log.trace("getting report")

    /**
      * Returns all possible combinations of users.
      *
      * @param relations relations
      */
    def createCombinations(relations: Seq[Relation]): Future[Seq[Combination]] = {
      log.trace(s"\tcreating combinations for ${relations.map(r => s"from ${r.groupFrom.id} to ${r.groupTo.map(_.id)} form: ${r.form.id}")}")

      /**
        * Map from groupId to sequence of group users.
        */
      val getGroupToUsersMap: Future[Map[Long, Seq[User]]] = {
        val allGroups = (relations.map(_.groupFrom.id) ++ relations.flatMap(_.groupTo.map(_.id))).distinct
        log.trace(s"\tcreating GroupToUsersMap, allGroups = $allGroups")
        userService.getGroupIdToUsersMap(allGroups, includeDeleted = true)
      }

      /**
        * Map from templateId to Freezed Form.
        */
      val getTemplateIdToFreezedFormMap: Future[Map[Long, Form]] = {
        val allTemplates = relations.map(_.form.id).distinct
        log.trace(s"\tcreating TemplateIdToFreezedFormMap, allTEmplates = $allTemplates")
        futureSeqToMap {
          allTemplates.map { templateId =>
            log.trace(s"\t\tgetting freezed form for template $templateId")
            formService.getOrCreateFreezedForm(eventId, templateId)
              .run
              .map { maybeForm =>
                val form = maybeForm.getOrElse(throw new NoSuchElementException("missed form"))
                (templateId, form)
              }
          }
        }
      }

      /**
        * Returns combinations of users from relations using given maps.
        */
      def getCombinations(groupToUsers: Map[Long, Seq[User]], templateIdToFreezedForm: Map[Long, Form]): Seq[Combination] = {
        relations.flatMap { relation =>
          for {
            userFrom <- groupToUsers(relation.groupFrom.id)
            userTo <- relation.groupTo match {
              case Some(groupTo) => groupToUsers(groupTo.id).map(Some(_))
              case None => Seq(None)
            }
          } yield {
            val form = templateIdToFreezedForm(relation.form.id)

            log.trace(s"\treturning Combination userFrom:${userFrom.id}, userTo:${userTo.map(_.id)}, form:${form.id}")
            Combination(userFrom, userTo, form)
          }
        }
      }

      for {
        groupToUsersMap <- getGroupToUsersMap
        templateIdToFreezedFormMap <- getTemplateIdToFreezedFormMap
      } yield getCombinations(groupToUsersMap, templateIdToFreezedFormMap)
    }


    /**
      * Returns report for the assessed user.
      *
      * @param assessedUser             assessed user (userTo)
      * @param assessedUserCombinations combinations with assessed user
      */
    def getReportForAssessedUser(assessedUser: Option[User], assessedUserCombinations: Seq[Combination]) = {
      log.trace(s"creating report for assessed user:${assessedUser.map(_.id)}, combinations:${assessedUserCombinations.map(c => s"userFrom ${c.userFrom.id} userTo ${c.userTo.map(_.id)} form ${c.form.id}")}")


      /**
        * Returns pairs of users with their answers.
        *
        * @param form      form to get answers for
        * @param usersFrom list of users from groupFrom
        */
      def getUserFromToAnswer(form: Form, usersFrom: Seq[User]) = {
        log.trace(s"\tcreating UserFromToAnswerMap for form:${form.id}, usersFroms:${usersFrom.map(_.id)}")

        val maybeAnswersSeqFuture = Future.sequence {
          usersFrom.map { userFrom =>
            log.trace(s"\t\tgetting answer for userFrom:${userFrom.id}, userTo:${assessedUser.map(_.id)}, form:${form.id}")
            answerDao
              .getAnswer(eventId, projectId, userFrom.id, assessedUser.map(_.id), form.id)
              .map {
                case Some(answer) => Some((userFrom, answer))
                case None => None
              }
          }
        }

        maybeAnswersSeqFuture.map { maybeAnswersSeq =>
          maybeAnswersSeq.collect { case (Some(v)) => v }
        }
      }

      /**
        * Returns report for single form.
        *
        * @param form              form to return report for
        * @param userFromToAnswers pairs of users with their answers
        */
      def getFormReport(form: Form, userFromToAnswers: Seq[(User, Answer.Form)]) = {
        log.trace(s"\tcreating form report userTo:${assessedUser.map(_.id)}, form:${form.id}, userAnswers:${userFromToAnswers.map(x => s"userFrom:${x._1.id}, answer: ${x._2}")}")
        val formElementIdToUserAnswerMap =
          userFromToAnswers
            .flatMap { x =>
              x._2
                .answers
                .map { answer =>
                  (answer.elementId, (x._1, answer))
                }
            }

        import Report._

        val formAnswers = form.elements.map { formElement =>
          val answers = formElementIdToUserAnswerMap
            .filter(_._1 == formElement.id)
            .map { case (_, (user, answer)) =>
              FormElementAnswerReport(user, answer)
          }
          FormElementReport(formElement, answers)
        }
        FormReport(form, formAnswers)
      }
      /**
        * Map from form to combinations with its form.
        */
      val formsToCombinations: Map[Form, Seq[Combination]] = assessedUserCombinations.groupBy(_.form)

      Future.sequence {
        formsToCombinations.map { case (form, formsCombinations) =>
          getUserFromToAnswer(form, formsCombinations.map(_.userFrom).distinct).map { userFromToAnswersMap =>
            getFormReport(form, userFromToAnswersMap)
          }
        }
      }.map { formReports =>
        Report(assessedUser, formReports.toSeq)
      }
    }

    for {
      relations <- relationDao.getList(optProjectId = Some(projectId))
      combinations <- createCombinations(relations.data)
      assesedUserToCombinations = combinations.groupBy(_.userTo)
      reports <- Future.sequence {
        assesedUserToCombinations.map {
          case (assessedUser, userCombinations) => getReportForAssessedUser(assessedUser, userCombinations)
        }
      }
    } yield {
      log.debug(s"report for event:$eventId project: $projectId created!")
      reports.toSeq
    }
  }

  /**
    * Returns aggregated report from report.
    *
    * @param report report
    */
  def getAggregatedReport(report: Report): AggregatedReport = {
    /**
      * Returns total count of answers.
      */
    def aggregateAnswersCount(answers: Seq[Answer.Element]) = {
      val total = answers.length
      if (total > 0) s"total: $total"
      else ""
    }

    /**
      * Returns percent of each text in answer.
      */
    def aggregateEachTextPercent(answers: Seq[Answer.Element]) = {
      val textsWithCount = answers
        .flatMap(_.text)
        .groupBy(identity)
        .mapValues(_.length)

      val totalCount = textsWithCount.values.sum

      val captionsToPercents = textsWithCount
        .toSeq
        .map { case (text, count) =>
          val percent = count * 100F / totalCount
          f""""$text" - $percent%3.2f%%"""
        }

      captionsToPercents.mkString(";\n")
    }

    /**
      * Returns percent of each value in answer.
      */
    def aggregateEachValuePercent(answers: Seq[Answer.Element], form: Form) = {
      val valueIdToCaption = form.elements.flatMap(_.values.map(v => (v.id, v.caption))).toMap

      val valueIdToCount = answers
        .flatMap(_.valuesIds)
        .flatten
        .groupBy(identity)
        .mapValues(_.length)

      val totalCount = valueIdToCount.values.sum

      val captionsToPercents = valueIdToCount
        .toSeq
        .map { case (valueId, count) =>
          val caption = valueIdToCaption.getOrElse(valueId, valueId.toString)
          val percent = count * 100F / totalCount
          f""""$caption" - $percent%3.2f%%"""
        }

      captionsToPercents.mkString(";\n")
    }

    import AggregatedReport._

    val forms = report.forms.map { formReport =>
      val elements = formReport.answers.map { answerReport =>
        val answers = answerReport.elementAnswers.map(_.answer)
        import Form.ElementKind._
        val result = answerReport.formElement.kind match {
          case TextArea | TextField => aggregateAnswersCount(answers)
          case Checkbox => aggregateEachTextPercent(answers)
          case CheckboxGroup | Radio | Select | LikeDislike => aggregateEachValuePercent(answers, formReport.form)
        }
        FormElementAnswer(answerReport.formElement, result)
      }
      FormAnswer(formReport.form, elements)
    }
    AggregatedReport(report.assessedUser, forms)
  }

  /**
    * Converts seq of futures of pairs to future of map.
    */
  private def futureSeqToMap[A, T, U](futures: Seq[Future[A]])(implicit ev: A <:< (T, U)): Future[Map[T, U]] = {
    Future.sequence(futures).map(_.toMap)
  }
}

object ReportService {

  /**
    * A combination of user from groupTo, user from groupFrom and form.    *
    *
    * @param userTo   user from groupTo
    * @param userFrom user from groupFrom
    * @param form     freezed form
    */
  case class Combination(userFrom: User, userTo: Option[User], form: Form)

}
