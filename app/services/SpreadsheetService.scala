package services

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

    /**
      * Returns rows with aggregated report.
      */
    val aggregationRows: Seq[RowData] = {
      val formElementIds = forms.flatMap(x => x.elements.map(_.id))

      val answers = aggregatedReports.map { report =>
        val userCell = textCell(report.assessedUser.flatMap(_.name).getOrElse(""))

        val elementIdToAnswer: Map[Long, String] = {
          for {
            form <- report.forms
            answer <- form.answers
          } yield (answer.element.id, answer.aggregationResult)
        }.toMap

        val answersCells = formElementIds.map(elementId => textCell(elementIdToAnswer.getOrElse(elementId, "")))

        row(userCell +: answersCells)
      }

      val header = row(Seq(textCell("Aggregated result", bold = true)))

      (header +: emptyRow +: getFormHeader(forms)) ++ answers
    }

    /**
      * Returns rows with manyToOne report.
      */
    val manyToOneRows: Seq[RowData] = {

      /**
        * Returns rows for single section in manyToOne report.
        *
        * @param report single report
        */
      def getManyToOneSection(report: Report) = {
        val toUserRow = row(Seq(textCell(report.assessedUser.flatMap(_.name).getOrElse(""))))

        val answers: Seq[(Long, User, String)] = for {
          form <- report.forms
          answer <- form.answers
          element <- answer.elementAnswers
        } yield (answer.formElement.id, element.fromUser, element.answer.getText(answer.formElement))

        val sectionForms = report.forms.map(_.form)
        val formElementIds = sectionForms.flatMap(_.elements.map(_.id))

        val users = answers.map(_._2).distinct
        val body = users.map { fromUser =>
          val elementIdToAnswer: Map[Long, String] = answers
            .collect { case (eId, u, v) if u.id == fromUser.id => (eId, v) }.toMap

          val cells = formElementIds.map { elementId =>
            val cellValue = elementIdToAnswer.get(elementId)
            textCell(cellValue.getOrElse(""))
          }
          row(textCell(fromUser.name.getOrElse("")) +: cells)
        }

        (toUserRow +: getFormHeader(sectionForms)) ++ body
      }

      val header = row(Seq(textCell("Many to one result", bold = true)))

      header +: reports.flatMap { report =>
        emptyRow +: getManyToOneSection(report)
      }
    }

    new BatchUpdateSpreadsheetRequest()
      .setRequests(Seq(
        addSheetRequest(1, "Aggregation"),
        updateCellsRequest(1, aggregationRows),
        addSheetRequest(2, "Many-One"),
        updateCellsRequest(2, manyToOneRows),
        deleteSheetRequest(0) // removes default sheet
      ))
  }

  /**
    * Empty spreadsheet cell.
    */
  private val emptyCell = new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(""))

  /**
    * Returns string spreadsheet cell.
    *
    * @param text cell value
    * @param bold set to bold
    */
  private def textCell(text: String, bold: Boolean = false) = {
    val cell = new CellData()
      .setUserEnteredValue(
        new ExtendedValue()
          .setStringValue(text)
      )
    if (bold) {
      cell.setUserEnteredFormat(
        new CellFormat()
          .setTextFormat(
            new TextFormat()
              .setBold(true)
          )
      )
    }
    cell
  }

  /**
    * Returns row.
    *
    * @param values row cells
    */
  private def row(values: Seq[CellData]) = new RowData().setValues(values)

  /**
    * Empty spreadsheet row.
    */
  private val emptyRow = row(Seq(emptyCell))

  /**
    * Returns answer table header.
    *
    * @param forms seq of forms
    */
  private def getFormHeader(forms: Seq[Form]) = {
    val formHeaderCells = emptyCell +: forms.filter(_.elements.nonEmpty).flatMap { form =>
      textCell(form.name, bold = true) +: form.elements.indices.map(_ => emptyCell)
    }
    val answerHeaderCells = textCell("Name", bold = true) +: forms.flatMap { form =>
      form.elements.map(x => textCell(x.caption, bold = true))
    }
    Seq(row(formHeaderCells), row(answerHeaderCells))
  }

  /**
    * Removes sheet by ID.
    */
  private def deleteSheetRequest(sheetId: Int) = new Request().setDeleteSheet(
    new DeleteSheetRequest().setSheetId(sheetId)
  )

  /**
    * Adds sheet to spreadsheet.
    *
    * @param sheetId ID of sheet
    * @param title   title of sheet
    */
  private def addSheetRequest(sheetId: Int, title: String) = {
    new Request()
      .setAddSheet(
        new AddSheetRequest()
          .setProperties(
            new SheetProperties()
              .setSheetId(sheetId)
              .setTitle(title)
          )
      )
  }

  /**
    * Updates cells in sheet.
    *
    * @param sheetId sheet ID
    * @param rows    rows
    */
  private def updateCellsRequest(sheetId: Int, rows: Seq[RowData]) = {
    val startCoord = new GridCoordinate().setSheetId(sheetId).setColumnIndex(0).setRowIndex(0)

    new Request().setUpdateCells(
      new UpdateCellsRequest()
        .setStart(startCoord)
        .setRows(rows)
        .setFields("userEnteredValue,userEnteredFormat.textFormat.bold")
    )
  }
}
