package services.spreadsheet

import Element._

/**
  * Action to apply on spreadsheet.
  */
trait Action {

  /**
    * Region for action to apply.
    */
  def region: Region
}

/**
  * Action with the point.
  */
trait PointAction extends Action {

  /**
    * Point for action to apply.
    */
  def coordinate: Point

  override def region: Region = coordinate.toRegion
}

object Action {

  /**
    * Sets text of cell at coordinate.
    */
  case class SetCellText(coordinate: Point, cell: Cell) extends PointAction

  /**
    * Sets borders of region.
    */
  case class SetBorder(region: Region, border: Border) extends Action

  /**
    * Sets background color of region.
    */
  case class SetColor(region: Region, color: Color) extends Action {

    /**
      * Converts region to seq of individual cells.
      */
    def toSingleCells: Seq[SetColor] = {
      for {
        x <- region.topLeft.x to region.bottomRight.x
        y <- region.topLeft.y to region.bottomRight.y
      } yield SetColor(Point(x, y).toRegion, color)
    }
  }

  /**
    * Merges given region into single cell.
    */
  case class SetMerge(region: Region) extends Action

  /**
    * Returns actions for given element.
    *
    * @param element    element to create actions for
    * @param coordinate starting point, placement on spreadsheet
    */
  def getActions(element: Element, coordinate: Point): Seq[Action] = {
    def getContainerActions(container: Container) = {

      def getActionsForInnerElements(elements: Seq[Element], current: Point): Seq[Action] = {

        def getNewCoordinate(el: Element) = container.direction match {
          case Container.Direction.TopToDown => current.copy(y = current.y + el.height)
          case Container.Direction.LeftToRight => current.copy(x = current.x + el.width)
        }

        elements match {
          case Seq() => Nil
          case innerElement +: tail =>
            val newCoordinate = getNewCoordinate(innerElement)

            getActions(innerElement, current) ++ getActionsForInnerElements(tail, newCoordinate)
        }
      }

      val containerActions = container.getActions(coordinate)
      val innerElementsActions = getActionsForInnerElements(container.elements, coordinate)

      innerElementsActions ++ containerActions
    }

    element match {
      case NoElement => Nil
      case cell: Cell => cell.getActions(coordinate)
      case container: Container => getContainerActions(container)
    }
  }
}
