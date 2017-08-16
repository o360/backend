package services.spreadsheet

import services.BaseServiceTest

/**
  * Test for spreadsheet actions.
  */
class ActionTest extends BaseServiceTest {

  import Action._
  import Element._
  import Container.Direction._

  "getActions" should {
    "return actions for given element" in {
      val point = Point(0, 0)
      val elementsToActions = Seq(
        {
          val cell = Cell("JustTest")
          cell -> Seq(SetCellText(point, cell))
        }, {
          val container = Container(LeftToRight)(Cell("test1"), NoElement, Cell("test2"))
          container -> Seq(
            SetCellText(point, Cell("test1")),
            SetCellText(point.copy(x = point.x + 1, point.y), Cell("test2"))
          )
        }, {
          val container = Container(TopToDown)(Cell("test1"), Cell("test2"), NoElement)
          container -> Seq(
            SetCellText(point, Cell("test1")),
            SetCellText(point.copy(x = point.x, point.y + 1), Cell("test2"))
          )
        }, {
          val container = Container(TopToDown, color = Some(Color.lightBlue))(Cell("test1"), Cell("test2"))
          container -> Seq(
            SetColor(Region(point, Point(0, 1)), Color.lightBlue)
          )
        }, {
          val border = Border(Border.Style.Solid, Border.Placement.outer, Color.lightBlue)
          val container = Container(TopToDown, border = Some(border))(Cell("test1"), NoElement, Cell("test2"))
          container -> Seq(
            SetBorder(Region(point, Point(point.x + 1, point.y + 2)), border)
          )
        }, {
          Cell("test", mergeToRight = Some(4)) -> Seq(
            SetMerge(Region(point, Point(point.x + 5, point.y)))
          )
        }, {
          val container =
            Container(TopToDown)(Cell("1"), Cell("2"), NoElement, Cell("3"), Cell("4")).colorIfEven(Color.lightGray)
          container -> Seq(
            SetColor(Point(0, 1).toRegion, Color.lightGray),
            SetColor(Point(0, 3).toRegion, Color.lightGray)
          )
        }, {
          NoElement -> Nil
        }
      )

      elementsToActions.foreach {
        case (element, expectedActions) =>
          val actions = Action.getActions(element, point)
          actions must contain allElementsOf expectedActions
      }
    }
  }
}
