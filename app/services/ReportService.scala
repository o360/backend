package services

import javax.inject.{Inject, Singleton}

import models.assessment.Answer
import models.dao.{AnswerDao, UserDao}
import models.form.Form
import models.report._
import utils.Logger
import utils.implicits.RichEitherT._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Report service.
  */
@Singleton
class ReportService @Inject()(
  userDao: UserDao,
  formService: FormService,
  answerDao: AnswerDao,
  implicit val ec: ExecutionContext
) extends Logger {

  def getReport(activeProjectId: Long): Future[Seq[Report]] = {

    for {
      answers <- answerDao.getList(optActiveProjectId = Some(activeProjectId))
      allUsersIds = (answers.map(_.userFromId) ++ answers.flatMap(_.userToId)).distinct
      allUsersMap <- userDao
        .getList(optIds = Some(allUsersIds), includeDeleted = true)
        .map(_.data.map(u => (u.id, u)).toMap)

      allFormsMap <- answers
        .map(_.form.id)
        .map(formService.getById)
        .sequenced
        .run
        .map(_.getOrElse(throw new NoSuchElementException("missed form")))
        .map(_.map(f => (f.id, f)).toMap)

      reports = answers.groupBy(_.userToId).map {
        case (userToId, userAnswers) =>
          val userTo = userToId.flatMap(allUsersMap.get)

          val formReports = userAnswers
            .groupBy(_.form.id)
            .map {
              case (formId, formAnswers) =>
                val form = allFormsMap(formId)
                val formElementsReport = form.elements.map { formElement =>
                  val elementAnswerReport = formAnswers.flatMap { answer =>
                    answer.elements.filter(_.elementId == formElement.id).map { elementAnswer =>
                      val userFrom = allUsersMap(answer.userFromId)
                      Report.FormElementAnswerReport(userFrom, elementAnswer, answer.isAnonymous)
                    }
                  }
                  Report.FormElementReport(formElement, elementAnswerReport)
                }
                Report.FormReport(form, formElementsReport)
            }
            .toSeq
          Report(userTo, formReports)
      }

    } yield {
      log.debug(s"report for active project: $activeProjectId created!")
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

      val captionsToPercents = textsWithCount.toSeq
        .map {
          case (text, count) =>
            val percent = count * 100F / totalCount
            f""""$text" - $count ($percent%3.2f%%)"""
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

      val captionsToPercents = valueIdToCount.toSeq
        .map {
          case (valueId, count) =>
            val caption = valueIdToCaption.getOrElse(valueId, valueId.toString)
            val percent = count * 100F / totalCount
            f""""$caption" - $count ($percent%3.2f%%)"""
        }

      captionsToPercents.mkString(";\n")
    }

    import AggregatedReport._

    val forms = report.forms.filter(_.form.showInAggregation).map { formReport =>
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
}
