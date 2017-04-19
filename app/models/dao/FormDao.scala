package models.dao

import javax.inject.{Inject, Singleton}

import models.ListWithTotal
import models.form.{Form, FormShort}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import slick.driver.JdbcProfile
import utils.listmeta.ListMeta

import scala.concurrent.Future


trait FormComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  class FormTable(tag: Tag) extends Table[FormShort](tag, "form") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def name = column[String]("name")

    def * = (id, name) <> ((FormShort.apply _).tupled, FormShort.unapply)
  }

  val Forms = TableQuery[FormTable]

  private val kindMapping: Map[Int, Form.ElementKind] = {
    import Form.ElementKind._
    Map(
      0 -> TextField,
      1 -> TextArea,
      2 -> Checkbox,
      3 -> CheckboxGroup,
      4 -> Radio,
      5 -> Select
    )
  }
  implicit lazy val kindColumnType = MappedColumnType.base[Form.ElementKind, Byte](
    x => kindMapping.find(_._2 == x).get._1.toByte,
    kindMapping(_)
  )

  /**
    * Db model for form element.
    */
  case class DbFormElement(
    id: Long,
    formId: Long,
    kind: Form.ElementKind,
    caption: String,
    defaultValue: Option[String],
    required: Boolean,
    order: Int
  ) {

    def toModel(values: Seq[Form.ElementValue]) = Form.Element(
      kind,
      caption,
      defaultValue,
      required,
      values
    )
  }

  object DbFormElement {

    def fromModel(formId: Long, element: Form.Element, order: Int): DbFormElement = DbFormElement(
      0,
      formId,
      element.kind,
      element.caption,
      element.defaultValue,
      element.required,
      order
    )
  }

  class FormElementTable(tag: Tag) extends Table[DbFormElement](tag, "form_element") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def formId = column[Long]("form_id")
    def kind = column[Form.ElementKind]("kind")
    def caption = column[String]("caption")
    def defaultValue = column[Option[String]]("default_value")
    def required = column[Boolean]("required")
    def order = column[Int]("ord")

    def * = (id, formId, kind, caption, defaultValue, required, order) <>
      ((DbFormElement.apply _).tupled, DbFormElement.unapply)
  }

  val FormElements = TableQuery[FormElementTable]

  /**
    * Db model for form element value.
    */
  case class DbFormElementValue(
    id: Long,
    elementId: Long,
    value: String,
    caption: String,
    order: Int
  ) {

    def toModel = Form.ElementValue(
      value,
      caption
    )
  }

  class FormElementValueTable(tag: Tag) extends Table[DbFormElementValue](tag, "form_element_value") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def elementId = column[Long]("element_id")
    def value = column[String]("value")
    def caption = column[String]("caption")
    def order = column[Int]("ord")

    def * = (id, elementId, value, caption, order) <> ((DbFormElementValue.apply _).tupled, DbFormElementValue.unapply)
  }

  val FormElementValues = TableQuery[FormElementValueTable]
}

/**
  *
  */
@Singleton
class FormDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
  with FormComponent with DaoHelper {

  import driver.api._

  /**
    * Returns list of forms.
    *
    * @param meta sorting and pagination
    */
  def getList()(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[FormShort]] = {
    runListQuery(Forms) {
      form => {
        case 'id => form.id
        case 'name => form.name
      }
    }
  }

  /**
    * Creates form.
    *
    * @param form form model
    * @return created form model with ID
    */
  def create(form: FormShort): Future[FormShort] = {
    db.run((Forms returning Forms.map(_.id) into ((item, id) => item.copy(id = id))) += form)
  }

  /**
    * Returns form with elements by ID.
    */
  def findById(id: Long): Future[Option[Form]] = {
    val query = Forms
      .filter(_.id === id)
      .joinLeft {
        FormElements
          .joinLeft(FormElementValues)
          .on {
            case (element, value) => element.id === value.elementId
          }
      }.on {
      case (form, (element, _)) => form.id === element.formId
    }

    db.run(query.result).map { flatResults =>
      flatResults.headOption.map { case (form, _) =>
        val elements = flatResults
          .collect { case (_, Some(elementWithValues)) => elementWithValues }
          .groupBy { case (element, _) => element }
          .toSeq
          .sortBy { case (element, _) => element.order }
          .map { case (element, valuesWithElements) =>
            val values = valuesWithElements
              .collect { case (_, Some(value)) => value }
              .sortBy(_.order)
              .map(_.toModel)

            element.toModel(values)
          }
        form.toModel(elements)
      }
    }
  }

  /**
    * Creates form elements.
    *
    * @param formId   ID of form template.
    * @param elements elements
    * @return created elements
    */
  def createElements(formId: Long, elements: Seq[Form.Element]): Future[Seq[Form.Element]] = {

    val dbModels = elements
      .zipWithIndex
      .map { case (element, index) =>
        val values = element
          .values
          .zipWithIndex
          .map { case (value, valueIndex) =>
            DbFormElementValue(id = 0, elementId = 0, value.value, value.caption, valueIndex)
          }

        (DbFormElement.fromModel(formId, element, index), values)
      }

    val resultsF = dbModels.map { case (element, values) =>
      val elementIdF = db.run(FormElements.returning(FormElements.map(_.id)) += element)
      if (values.nonEmpty) {
        elementIdF.flatMap { elementId: Long =>
          db.run(FormElementValues ++= values.map(_.copy(elementId = elementId)))
        }
      } else {
        elementIdF
      }
    }

    Future.sequence(resultsF).map(_ => elements)
  }

  /**
    * Deletes form template with elements.
    *
    * @param id form template ID
    * @return number of rows affected
    */
  def delete(id: Long): Future[Int] = db.run {
    Forms.filter(_.id === id).delete
  }

  /**
    * Deletes form elements.
    *
    * @param formId form template ID
    */
  def deleteElements(formId: Long): Future[Int] = db.run {
    FormElements.filter(_.formId === formId).delete
  }

  /**
    * Updates form.
    *
    * @param form form model
    * @return updated form
    */
  def update(form: FormShort): Future[FormShort] = db.run {
    Forms.filter(_.id === form.id).update(form)
  }.map(_ => form)
}
