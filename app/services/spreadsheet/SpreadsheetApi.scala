package services.spreadsheet

import com.google.api.services.sheets.v4.model._

import scala.collection.JavaConverters._
import Action._
import Element.{Cell, Point, Region}

import scala.util.Try

object SpreadsheetApi {

  private val spreadsheetDefaultWidth = 26
  private val spreadsheetDefaultHeight = 1000

  /**
    * Returns spreadsheet API requests.
    *
    * @param actions              actions to execute
    * @param sheetId              ID of sheet
    * @param freezedColumnsAmount amount of freezed columns
    */
  def getRequests(actions: Seq[Action], sheetId: Int, freezedColumnsAmount: Int = 0): Seq[Request] = {

    val maxColumn = Try(actions.map(_.region.bottomRight.x).max).toOption.getOrElse(0)
    val maxRow = Try(actions.map(_.region.bottomRight.y).max).toOption.getOrElse(0)

    val updateCellsRequest = {

      val coordinateToColor: Map[Point, Element.Color] = actions
        .collect { case c: SetColor => c }
        .sortBy(_.region.area) // smaller regions have priority
        .flatMap(_.toSingleCells)
        .groupBy(_.region.topLeft)
        .mapValues(_.head.color)

      val setTextActions = actions.collect { case v: SetCellText => v }

      val rows = setTextActions.groupBy(_.coordinate.y)
      if (rows.isEmpty) None
      else {
        val rowsModels = (0 to maxRow).map { rowIndex =>
          val cells = rows.getOrElse(rowIndex, Nil).groupBy(_.coordinate.x).mapValues(_.head.cell)
          val cellsModels = (0 to maxColumn).map { cellIndex =>
            val color = coordinateToColor.get(Point(cellIndex, rowIndex))
            cells.get(cellIndex).fold(textCell("", color = color)) { cell =>
              textCell(cell.text, cell.alignment, cell.format, color)
            }
          }
          new RowData().setValues(cellsModels.asJava)
        }

        val startCoordinate = new GridCoordinate().setSheetId(sheetId).setColumnIndex(0).setRowIndex(0)

        val request = new Request().setUpdateCells(
          new UpdateCellsRequest()
            .setStart(startCoordinate)
            .setRows(rowsModels.asJava)
            .setFields(
              "userEnteredValue,userEnteredFormat.textFormat,userEnteredFormat.backgroundColor,userEnteredFormat.horizontalAlignment")
        )

        Some(request)
      }
    }

    val cellsMergeRequests = {
      val createMergeActions = actions.collect { case a: SetMerge => a }

      createMergeActions.map { createMergeAction =>
        new Request()
          .setMergeCells(
            new MergeCellsRequest()
              .setRange(getGridRange(createMergeAction.region, sheetId, includeRight = true))
              .setMergeType("MERGE_ALL")
          )
      }
    }

    val bordersRequests = {
      val setBordersActions = actions.collect { case a: SetBorder => a }.sortBy(_.region.area)

      setBordersActions.map { setBorderAction =>
        val border = setBorderAction.border
        val req = new UpdateBordersRequest()
          .setRange(getGridRange(setBorderAction.region, sheetId, includeRight = false))

        val borderModel = new Border()
          .setStyle(border.style.value)
          .setColor(getApiColor(border.color))

        import Element.Border.Placement._

        val placement = border.placement
        if (placement.contains(Left)) req.setLeft(borderModel)
        if (placement.contains(Right)) req.setRight(borderModel)
        if (placement.contains(Top)) req.setTop(borderModel)
        if (placement.contains(Bottom)) req.setBottom(borderModel)
        if (placement.contains(InnerHorizontal)) req.setInnerHorizontal(borderModel)
        if (placement.contains(InnerVertical)) req.setInnerVertical(borderModel)

        new Request().setUpdateBorders(req)
      }
    }

    val freezedColummsRequest = {
      if (freezedColumnsAmount == 0) None
      else Some(getFreezeColumnRequest(sheetId, freezedColumnsAmount))
    }

    val spreadsheetSizeRequest = {
      if (maxColumn >= spreadsheetDefaultWidth && maxRow >= spreadsheetDefaultHeight)
        Some(getResizeSpreadsheetRequest(sheetId, maxColumn + 1, maxRow + 1))
      else if (maxColumn >= spreadsheetDefaultWidth)
        Some(getResizeSpreadsheetRequest(sheetId, width = maxColumn + 1))
      else if (maxRow >= spreadsheetDefaultHeight)
        Some(getResizeSpreadsheetRequest(sheetId, height = maxRow + 1))
      else None
    }

    (spreadsheetSizeRequest ++ updateCellsRequest ++ cellsMergeRequests ++ bordersRequests ++ freezedColummsRequest).toSeq
  }

  /**
    * Adds sheet to spreadsheet.
    *
    * @param sheetId ID of sheet
    * @param title   title of sheet
    */
  def addSheetRequest(sheetId: Int, title: String): Request = {
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
    * Removes sheet by ID.
    */
  def deleteSheetRequest(sheetId: Int): Request = {
    new Request().setDeleteSheet(
      new DeleteSheetRequest().setSheetId(sheetId)
    )
  }

  /**
    * Returns API color model from element color.
    */
  private def getApiColor(c: Element.Color) =
    new Color()
      .setRed(c._1 / 255F)
      .setGreen(c._2 / 255F)
      .setBlue(c._3 / 255F)

  /**
    * Returns cell API model.
    */
  private def textCell(
    text: String,
    alignment: Option[Cell.Alignment] = None,
    format: Option[Cell.Format.ValueSet] = None,
    color: Option[Element.Color] = None
  ) = {

    val cell = new CellData()
      .setUserEnteredValue(
        new ExtendedValue()
          .setStringValue(text)
      )

    if (alignment.nonEmpty || format.nonEmpty || color.nonEmpty) {
      val cellFormat = new CellFormat()

      alignment.foreach(a => cellFormat.setHorizontalAlignment(a.value))

      format.foreach { f =>
        val textFormat = new TextFormat()
        import Cell.Format._
        if (f.contains(Bold)) textFormat.setBold(true)
        if (f.contains(Italic)) textFormat.setItalic(true)
        if (f.contains(Strikethrough)) textFormat.setStrikethrough(true)
        if (f.contains(Underline)) textFormat.setUnderline(true)

        cellFormat.setTextFormat(textFormat)
      }

      color.foreach(c => cellFormat.setBackgroundColor(getApiColor(c)))

      cell.setUserEnteredFormat(cellFormat)
    }

    cell
  }

  /**
    * Returns grid range for given region and sheet ID.
    */
  private def getGridRange(region: Region, sheetId: Int, includeRight: Boolean) = {
    new GridRange()
      .setStartRowIndex(region.topLeft.y)
      .setStartColumnIndex(region.topLeft.x)
      .setEndRowIndex(region.bottomRight.y + (if (includeRight) 1 else 0))
      .setEndColumnIndex(region.bottomRight.x)
      .setSheetId(sheetId)
  }

  /**
    * Freeze first column for sheetID.
    *
    * @param sheetId ID of sheet
    */
  private def getFreezeColumnRequest(sheetId: Int, amount: Int) = {
    new Request()
      .setUpdateSheetProperties(
        new UpdateSheetPropertiesRequest()
          .setFields("gridProperties.frozenColumnCount")
          .setProperties(
            new SheetProperties()
              .setSheetId(sheetId)
              .setGridProperties(
                new GridProperties()
                  .setFrozenColumnCount(amount)
              )
          )
      )
  }

  /**
    * Resize sheet.
    *
    * @param sheetId ID of sheet
    * @param width   desired width
    * @param height  desired height
    */
  private def getResizeSpreadsheetRequest(
    sheetId: Int,
    width: Int = spreadsheetDefaultWidth,
    height: Int = spreadsheetDefaultHeight
  ) = {
    new Request()
      .setUpdateSheetProperties(
        new UpdateSheetPropertiesRequest()
          .setFields("gridProperties.rowCount,gridProperties.columnCount")
          .setProperties(
            new SheetProperties()
              .setSheetId(sheetId)
              .setGridProperties(
                new GridProperties()
                  .setColumnCount(width)
                  .setRowCount(height)
              )
          )
      )
  }
}
