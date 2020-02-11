package services.spreadsheet

import services.spreadsheet.Action._
import services.spreadsheet.Element._

/**
  * Spreadsheet element.
  */
trait Element {

  /**
    * Width in cells.
    */
  def width: Int

  /**
    * Height in cells.
    */
  def height: Int

  /**
    * Border.
    */
  def border: Option[Border]

  /**
    * Background color.
    */
  def color: Option[Color]

  /**
    * Returns actions.
    *
    * @param topLeft top left position of element
    */
  def getActions(topLeft: Point): Seq[Action] = {
    val borderAction = border.map { b =>
      val bottomRight = Point(topLeft.x + width, topLeft.y + height)
      SetBorder(Region(topLeft, bottomRight), b)
    }
    val colorActions = color.map { c =>
      val bottomRight = Point(topLeft.x + width - 1, topLeft.y + height - 1)

      SetColor(Region(topLeft, bottomRight), c)
    }
    borderAction.toSeq ++ colorActions
  }

  /**
    * Copies element replacing color.
    */
  def copyWithColor(color: Option[Color]): Element
}

object Element {

  /**
    * Color of element in RGB format (0 - 255).
    */
  type Color = (Int, Int, Int)

  object Color {
    val black: Color = (0, 0, 0)
    val lightBlue: Color = (173, 216, 230)
    val lightGreen: Color = (173, 230, 216)
    val lightGray: Color = (230, 230, 235)
  }

  /**
    * Element border.
    */
  case class Border(style: Border.Style, placement: Border.Placement.ValueSet, color: Color = Color.black)
  object Border {

    /**
      * Border placement relative to element center.
      */
    object Placement extends Enumeration {
      val Left, Right, Top, Bottom, InnerHorizontal, InnerVertical = Value

      val outer = ValueSet(Left, Right, Top, Bottom)
      val inner = ValueSet(InnerVertical, InnerHorizontal)
    }

    /**
      * Border style.
      */
    trait Style {
      def value: String
    }
    object Style {
      case object Solid extends Style {
        def value = "SOLID"
      }
      case object SolidMedium extends Style {
        def value = "SOLID_MEDIUM"
      }
      case object SolidThick extends Style {
        def value = "SOLID_THICK"
      }
    }
  }

  /**
    * Coordinate on spreadsheet.
    */
  case class Point(x: Int, y: Int) {

    /**
      * Returns region contains one cell.
      */
    def toRegion = Region(this, this)
  }

  /**
    * Region on spreadsheet.
    */
  case class Region(topLeft: Point, bottomRight: Point) {
    lazy val area = {
      val width = bottomRight.x - topLeft.x
      val height = bottomRight.y - topLeft.y
      width * height
    }
  }

  /**
    * Container for cells (stack panel).
    *
    * @param direction direction of inner elements placement
    * @param elements  inner elements
    * @param border    container border
    * @param color     container back color
    */
  case class Container(
    elements: Seq[Element],
    direction: Container.Direction,
    color: Option[Color],
    border: Option[Border]
  ) extends Element {
    def width: Int = direction match {
      case Container.Direction.TopToDown   => if (elements.isEmpty) 0 else elements.map(_.width).max
      case Container.Direction.LeftToRight => if (elements.isEmpty) 0 else elements.map(_.width).sum
    }

    def height: Int = direction match {
      case Container.Direction.TopToDown   => if (elements.isEmpty) 0 else elements.map(_.height).sum
      case Container.Direction.LeftToRight => if (elements.isEmpty) 0 else elements.map(_.height).max
    }

    /**
      * Colors each even inner element.
      */
    def colorIfEven(color: Color): Container = this.copy(
      elements = elements.filter(el => el.width > 0 || el.height > 0).zipWithIndex.map {
        case (el, index) =>
          el.copyWithColor(if (index % 2 == 0) None else Some(color))
      }
    )
    override def copyWithColor(color: Option[(Int, Int, Int)]): Element = copy(color = color)
  }
  object Container {

    /**
      * Stacking direction.
      */
    sealed trait Direction
    object Direction {

      /**
        * Stack inner elements from top to down.
        */
      case object TopToDown extends Direction

      /**
        * Stack inner elements from left to right.
        */
      case object LeftToRight extends Direction
    }

    def apply(
      direction: Container.Direction,
      color: Option[Color] = None,
      border: Option[Border] = None
    )(elements: Element*): Container = Container(elements.toSeq, direction, color, border)
  }

  /**
    * Spreadsheet cell.
    *
    * @param text         text
    * @param border       border
    * @param color        background color
    * @param mergeToRight amount of cells to the right for merge to current
    */
  case class Cell(
    text: String,
    border: Option[Border] = None,
    color: Option[Color] = None,
    alignment: Option[Cell.Alignment] = None,
    format: Option[Cell.Format.ValueSet] = None,
    mergeToRight: Option[Int] = None
  ) extends Element {
    override def width: Int = mergeToRight.getOrElse(0) + 1

    override def height: Int = 1

    override def getActions(topLeft: Point): Seq[Action] = {
      val setTextAction = SetCellText(topLeft, this)
      val mergeAction = mergeToRight.map { cellsToMerge =>
        SetMerge(Region(topLeft, Point(topLeft.x + cellsToMerge + 1, topLeft.y)))
      }

      super.getActions(topLeft) ++ mergeAction.toSeq :+ setTextAction
    }
    override def copyWithColor(color: Option[(Int, Int, Int)]): Element = copy(color = color)
  }

  object Cell {

    /**
      * Horizontal text alignment.
      */
    trait Alignment {
      def value: String
    }
    object Alignment {
      case object Left extends Alignment {
        def value = "LEFT"
      }
      case object Center extends Alignment {
        def value = "CENTER"
      }
      case object Right extends Alignment {
        def value = "RIGHT"
      }
    }

    /**
      * Text format.
      */
    object Format extends Enumeration {
      val Bold, Italic, Strikethrough, Underline = Value

      val bold = ValueSet(Bold)
    }
    val empty = Cell("")
  }

  /**
    * Special element with zero size.
    */
  case object NoElement extends Element {
    override def width: Int = 0
    override def height: Int = 0
    override def border: Option[Border] = None
    override def color: Option[(Int, Int, Int)] = None
    override def copyWithColor(color: Option[(Int, Int, Int)]): Element = this
  }
}
