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

  implicit lazy val formKindColumnType = MappedColumnType.base[Form.Kind, Byte](
    {
      case Form.Kind.Active => 0
      case Form.Kind.Freezed => 1
    }, {
      case 0 => Form.Kind.Active
      case 1 => Form.Kind.Freezed
    }
  )

  class FormTable(tag: Tag) extends Table[FormShort](tag, "form") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def name = column[String]("name")
    def kind = column[Form.Kind]("kind")

    def * = (id, name, kind) <> ((FormShort.apply _).tupled, FormShort.unapply)
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
    required: Boolean,
    order: Int
  ) {

    def toModel(values: Seq[Form.ElementValue]) = Form.Element(
      id,
      kind,
      caption,
      required,
      values
    )
  }

  object DbFormElement {

    def fromModel(formId: Long, element: Form.Element, order: Int): DbFormElement = DbFormElement(
      element.id,
      formId,
      element.kind,
      element.caption,
      element.required,
      order
    )
  }

  class FormElementTable(tag: Tag) extends Table[DbFormElement](tag, "form_element") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def formId = column[Long]("form_id")
    def kind = column[Form.ElementKind]("kind")
    def caption = column[String]("caption")
    def required = column[Boolean]("required")
    def order = column[Int]("ord")

    def * = (id, formId, kind, caption, required, order) <>
      ((DbFormElement.apply _).tupled, DbFormElement.unapply)
  }

  val FormElements = TableQuery[FormElementTable]

  /**
    * Db model for form element value.
    */
  case class DbFormElementValue(
    id: Long,
    elementId: Long,
    caption: String,
    order: Int
  ) {

    def toModel = Form.ElementValue(
      id,
      caption
    )
  }

  class FormElementValueTable(tag: Tag) extends Table[DbFormElementValue](tag, "form_element_value") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def elementId = column[Long]("element_id")
    def caption = column[String]("caption")
    def order = column[Int]("ord")

    def * = (id, elementId, caption, order) <> ((DbFormElementValue.apply _).tupled, DbFormElementValue.unapply)
  }

  val FormElementValues = TableQuery[FormElementValueTable]
}

/**
  * Component for event_form_mapping table.
  */
trait EventFormMappingComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  /**
    * DB model for form event mapping.
    */
  case class DbEventFormMapping(
    eventId: Long,
    formTemplateId: Long,
    formFreezedId: Long
  )

  class EventFormMappingTable(tag: Tag) extends Table[DbEventFormMapping](tag, "event_form_mapping") {
    def eventId = column[Long]("event_id")
    def formTemplateId = column[Long]("form_template_id")
    def formFreezedId = column[Long]("form_freezed_id")

    def * = (eventId, formTemplateId, formFreezedId) <> ((DbEventFormMapping.apply _).tupled, DbEventFormMapping.unapply)
  }

  val EventFormMappings = TableQuery[EventFormMappingTable]
}

/**
  * Form DAO.
  */
@Singleton
class FormDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
  with FormComponent
  with EventFormMappingComponent
  with DaoHelper {

  import driver.api._

  /**
    * Returns list of forms.
    *
    * @param meta sorting and pagination
    */
  def getList(
    optKind: Option[Form.Kind] = None,
    optFormTemplateId: Option[Long] = None
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[FormShort]] = {
    val query = Forms
      .applyFilter { form =>
        Seq(
          optKind.map(form.kind === _),
          optFormTemplateId.map { formTemplateId =>
            form.id in EventFormMappings.filter(_.formTemplateId === formTemplateId).map(_.formFreezedId)
          }
        )
      }

    runListQuery(query) {
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
    db.run(Forms.returning(Forms.map(_.id)).into((item, id) => item.copy(id = id)) += form)
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
            DbFormElementValue(id = 0, elementId = 0, value.caption, valueIndex)
          }

        (DbFormElement.fromModel(formId, element, index), values)
      }

    val results = dbModels.map { case (element, values) =>
      val elementId = db.run(FormElements.returning(FormElements.map(_.id)) += element)
      if (values.nonEmpty) {
        elementId.flatMap { elementId: Long =>
          db.run(FormElementValues ++= values.map(_.copy(elementId = elementId)))
        }
      } else elementId
    }

    for {
      _ <- Future.sequence(results)
      form <- findById(formId)
    } yield form.get.elements
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

  /**
    * Returns ID of freezed form.
    *
    * @param eventId        ID of event
    * @param templateFormId ID of template form
    */
  def getFreezedFormId(eventId: Long, templateFormId: Long): Future[Option[Long]] = db.run {
    EventFormMappings
      .filter(x => x.eventId === eventId && x.formTemplateId === templateFormId)
      .map(_.formFreezedId)
      .result
      .headOption
  }

  /**
    * Sets freezed form ID
    *
    * @param eventId        ID of event
    * @param templateFormId ID of template form
    * @param freezedFormId  ID of freezed form
    */
  def setFreezedFormId(eventId: Long, templateFormId: Long, freezedFormId: Long): Future[Unit] = db.run {
    EventFormMappings += DbEventFormMapping(eventId, templateFormId, freezedFormId)
  }.map(_ => ())
}
