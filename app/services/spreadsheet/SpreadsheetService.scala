package services.spreadsheet

import javax.inject.{Inject, Singleton}

import com.google.api.services.sheets.v4.model._
import models.form.Form
import models.report.{AggregatedReport, Report}
import models.user.User

import scala.collection.JavaConversions._

/**
  * Spreadsheet service.
  */
@Singleton
class SpreadsheetService @Inject()() {

  import Element._
  import Container.Direction._

  /**
    * Returns batch spreadsheet request. Request contains operations for setting report data in spreadsheet.
    *
    * @param reports           reports
    * @param aggregatedReports aggregated reports
    * @param forms             forms used in relations
    */
  def getBatchUpdateRequest(
    reports: Seq[Report],
    aggregatedReports: Seq[AggregatedReport],
    forms: Seq[Form]
  ): BatchUpdateSpreadsheetRequest = {

    def getFormContainer(form: Form, answers: Element, headerColor: Color) =
      Container(
        TopToDown,
        border = Some(Border(Border.Style.SolidThick, Border.Placement.outer))
      )(
        Cell(
          form.name,
          border = Some(Border(Border.Style.Solid, Border.Placement.ValueSet(Border.Placement.Bottom))),
          alignment = Some(Cell.Alignment.Center),
          format = Some(Cell.Format.bold),
          color = Some(headerColor),
          mergeToRight = if (form.elements.length <= 1) None else Some(form.elements.length - 1)
        ),
        Container(
          LeftToRight,
          border = Some(
            Border(Border.Style.Solid,
                   Border.Placement.ValueSet(
                     Border.Placement.InnerVertical,
                     Border.Placement.Bottom
                   )))
        )(form.elements.map(element => Cell(element.caption, format = Some(Cell.Format.bold))): _*),
        answers
      )

    val aggregation: Element = {
      def getFormBody(reports: Seq[AggregatedReport], form: Form) = {
        val formElementIds = form.elements.map(_.id)
        val rows = reports.map { report =>
          val elementIdToAnswer: Map[Long, String] = {
            for {
              form <- report.forms
              answer <- form.answers
            } yield (answer.element.id, answer.aggregationResult)
          }.toMap

          val answersCells = formElementIds.map { elementId =>
            Cell(elementIdToAnswer.getOrElse(elementId, ""))
          }

          Container(
            Container.Direction.LeftToRight,
            border = Some(Border(Border.Style.Solid, Border.Placement.ValueSet(Border.Placement.InnerVertical)))
          )(answersCells: _*)
        }
        Container(TopToDown)(rows: _*).colorIfEven(Color.lightGray)
      }

      val reportsForUsers = aggregatedReports.filter(_.assessedUser.nonEmpty)
      val users = aggregatedReports.collect { case AggregatedReport(Some(user), _) => user }
      val userForms = aggregatedReports.filter(_.assessedUser.nonEmpty).flatMap(_.forms).map(_.form).distinct

      val reportsForSurvey = aggregatedReports.filter(_.assessedUser.isEmpty)
      val surveyForms = aggregatedReports.filter(_.assessedUser.isEmpty).flatMap(_.forms).map(_.form).distinct

      Container(TopToDown)(
        Cell("Aggregated result", format = Some(Cell.Format.bold)),
        Cell.empty,
        Container(LeftToRight)(
          Container(TopToDown)(
            Cell("Name", format = Some(Cell.Format.bold), color = Some(Color.lightBlue)),
            Cell.empty,
            Container(TopToDown)(
              users.map(user => Cell(user.name.getOrElse(""))): _*
            ).colorIfEven(Color.lightGray)
          ),
          Container(LeftToRight)(
            userForms.map { form =>
              val body = getFormBody(reportsForUsers, form)
              getFormContainer(form, body, Color.lightBlue)
            }: _*
          )
        ),
        Cell.empty,
        Cell.empty,
        if (surveyForms.nonEmpty && reportsForSurvey.nonEmpty)
          Container(LeftToRight)(
            Cell("Survey", format = Some(Cell.Format.bold), color = Some(Color.lightGreen)),
            Container(LeftToRight)(
              surveyForms.map { form =>
                val body = getFormBody(reportsForSurvey, form)
                getFormContainer(form, body, Color.lightGreen)
              }: _*
            )
          )
        else Cell.empty
      )
    }

    val manyToOne: Element = {
      def getSection(report: Report): Element = {
        val nonAnonymous =
          report.forms.flatMap(_.answers.flatMap(_.elementAnswers.filterNot(_.isAnonymous).map(_.fromUser))).distinct
        val anonymous =
          report.forms.flatMap(_.answers.flatMap(_.elementAnswers.filter(_.isAnonymous).map(_.fromUser))).distinct

        val anonymousUserNameMapping = anonymous.map(_.id).zipWithIndex.toMap.mapValues(x => s"Anonymous #$x")

        val fromUsers = nonAnonymous.map((_, false)) ++ anonymous.map((_, true))
        val forms = report.forms.map(_.form).distinct

        def getFormBody(form: Form) = {
          val answers: Seq[(Long, User, String, Boolean)] = for {
            form <- report.forms
            answer <- form.answers
            element <- answer.elementAnswers
          } yield
            (answer.formElement.id, element.fromUser, element.answer.getText(answer.formElement), element.isAnonymous)
          val formElementIds = form.elements.map(_.id)

          val rows = fromUsers.map {
            case (fromUser, isAnonUser) =>
              val elementIdToAnswer: Map[Long, String] = answers.collect {
                case (eId, u, v, a) if u.id == fromUser.id && a == isAnonUser => (eId, v)
              }.toMap

              val cells = formElementIds.map { elementId =>
                val cellValue = elementIdToAnswer.get(elementId)
                Cell(cellValue.getOrElse(""))
              }
              Container(
                LeftToRight,
                border = Some(Border(Border.Style.Solid, Border.Placement.ValueSet(Border.Placement.InnerVertical)))
              )(cells: _*)
          }
          Container(TopToDown)(rows: _*)
            .colorIfEven(Color.lightGray)
        }

        val surveyAnswers: Seq[(Form, Long, String)] = for {
          r <- reports if r.assessedUser.isEmpty
          form <- r.forms
          answer <- form.answers
          element <- answer.elementAnswers
          if !element.isAnonymous && report.assessedUser.fold(false)(_.id == element.fromUser.id)
        } yield (form.form, answer.formElement.id, element.answer.getText(answer.formElement))

        val surveyForms = surveyAnswers.map(_._1).distinct
        def getSurveyFormBody(form: Form) = {
          val elementIdToAnswer: Map[Long, String] = surveyAnswers
            .filter(_._1.id == form.id)
            .groupBy(_._2)
            .mapValues(_.head._3)

          val formElementIds = form.elements.map(_.id)

          Container(
            LeftToRight,
            border = Some(Border(Border.Style.Solid, Border.Placement.ValueSet(Border.Placement.InnerVertical)))
          )(
            formElementIds.map { elementId =>
              val cellValue = elementIdToAnswer.getOrElse(elementId, "")
              Cell(cellValue)
            }: _*
          )
        }

        val rowColor = if (report.assessedUser.isEmpty) Color.lightGreen else Color.lightBlue

        Container(TopToDown)(
          Cell.empty,
          Cell.empty,
          Container(LeftToRight)(
            Container(
              TopToDown,
              border = Some(Border(Border.Style.SolidThick, Border.Placement.outer))
            )(
              Cell(
                report.assessedUser.map(_.name.getOrElse("")).getOrElse("Survey"),
                format = Some(Cell.Format.bold),
                mergeToRight = Some(1),
                color = Some(rowColor)
              ),
              Container(LeftToRight)(
                Cell.empty,
                Container(TopToDown)(
                  Cell("Name", format = Some(Cell.Format.bold)),
                  Container(TopToDown)(
                    fromUsers.map {
                      case (u, true) => Cell(anonymousUserNameMapping.getOrElse(u.id, "Anonymous"))
                      case (u, false) => Cell(u.name.getOrElse(""))
                    }: _*
                  ).colorIfEven(Color.lightGray),
                  if (surveyAnswers.isEmpty) NoElement
                  else
                    Container(TopToDown)(
                      Cell.empty,
                      Cell("Survey", format = Some(Cell.Format.bold), color = Some(Color.lightGreen)),
                      Cell.empty,
                      Cell.empty
                    )
                )
              )
            ),
            Container(TopToDown)(
              Container(LeftToRight)(forms.map { form =>
                val body = getFormBody(form)
                getFormContainer(form, body, rowColor)
              }: _*),
              if (surveyAnswers.isEmpty) NoElement
              else
                Container(TopToDown)(
                  Cell.empty,
                  Container(LeftToRight)(
                    surveyForms.map { form =>
                      val body = getSurveyFormBody(form)
                      getFormContainer(form, body, Color.lightGreen)
                    }: _*
                  )
                )
            )
          )
        )
      }

      Container(TopToDown)(
        Cell("Many to one result", format = Some(Cell.Format.bold)) +:
          reports.map(getSection): _*
      )
    }

    val actionsAggregation = Action.getActions(aggregation, Point(0, 0))
    val requestsAggregation = SpreadsheetApi.getRequests(actionsAggregation, sheetId = 1, freezedColumnsAmount = 1)

    val actionsManyToOne = Action.getActions(manyToOne, Point(0, 0))
    val requestsManyToOne = SpreadsheetApi.getRequests(actionsManyToOne, sheetId = 2, freezedColumnsAmount = 2)

    new BatchUpdateSpreadsheetRequest()
      .setRequests(
        Seq(
          SpreadsheetApi.addSheetRequest(1, "Aggregation"),
          SpreadsheetApi.addSheetRequest(2, "Many-One"),
          SpreadsheetApi.deleteSheetRequest(0) // removes default sheet
        ) ++
          requestsManyToOne ++
          requestsAggregation
      )
  }
}
