package services.spreadsheet

import com.google.api.services.sheets.v4.model.Request
import services.BaseServiceTest

import scala.jdk.CollectionConverters._
import scala.util.Try

/**
  * Test for spreadsheet API.
  */
class SpreadsheetApiTest extends BaseServiceTest {

  import Action._
  import Element._

  "getRequests" should {
    "return requests for given actions" in {
      val actionToRequestPredicate: Seq[(Seq[Action], Request => Boolean)] = Seq(
        {
          (
            Seq(
              SetCellText(
                Point(0, 0),
                Cell("test", alignment = Some(Cell.Alignment.Center), format = Some(Cell.Format.bold))
              )
            ),
            request => {
              val cell = request.getUpdateCells.getRows.asScala.head.getValues.asScala.head
              cell.getUserEnteredValue.getStringValue == "test" &&
              cell.getUserEnteredFormat.getHorizontalAlignment == Cell.Alignment.Center.value &&
              cell.getUserEnteredFormat.getTextFormat.getBold == true
            }
          )
        }, {
          (
            Seq(SetCellText(Point(0, 0), Cell("")), SetColor(Point(0, 0).toRegion, Color.lightGray)),
            r => {
              val color =
                r.getUpdateCells.getRows.asScala.head.getValues.asScala.head.getUserEnteredFormat.getBackgroundColor
              color.getRed == Color.lightGray._1 / 255f &&
              color.getGreen == Color.lightGray._2 / 255f &&
              color.getBlue == Color.lightGray._3 / 255f
            }
          )
        }, {
          (
            Seq(
              SetBorder(
                Region(Point(0, 0), Point(2, 2)),
                Border(Border.Style.SolidMedium, Border.Placement.ValueSet(Border.Placement.Bottom))
              )
            ),
            r => {
              val borderProp = r.getUpdateBorders
              borderProp.getRange.getStartColumnIndex == 0 &&
              borderProp.getRange.getEndColumnIndex == 2 &&
              borderProp.getBottom.getStyle == Border.Style.SolidMedium.value
            }
          )
        }, {
          (Seq(SetMerge(Region(Point(0, 0), Point(5, 5)))), _.getMergeCells != null)
        }
      )

      actionToRequestPredicate.foreach {
        case (actions, pred) =>
          val requests = SpreadsheetApi.getRequests(actions, 1)

          def tryOrFalsePred(r: Request) = Try(pred(r)).getOrElse(false)
          requests.exists(tryOrFalsePred) mustBe true
      }
    }

    "return freeze columns request" in {
      val result = SpreadsheetApi.getRequests(Nil, 0, 5)

      val props = result.head.getUpdateSheetProperties.getProperties

      props.getSheetId mustBe 0
      props.getGridProperties.getFrozenColumnCount mustBe 5
    }
  }

  "add sheet request" should {
    "return proper request" in {
      val result = SpreadsheetApi.addSheetRequest(1, "test")

      val props = result.getAddSheet.getProperties

      props.getSheetId mustBe 1
      props.getTitle mustBe "test"
    }
  }

  "delete sheet request" should {
    "return proper request" in {
      val result = SpreadsheetApi.deleteSheetRequest(1)

      result.getDeleteSheet.getSheetId mustBe 1
    }
  }
}
