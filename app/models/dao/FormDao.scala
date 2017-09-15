package models.dao

import javax.inject.{Inject, Singleton}

import models.ListWithTotal
import models.form.{Form, FormShort}
import models.form.element._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import utils.listmeta.ListMeta
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

trait FormComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  implicit lazy val formKindColumnType = MappedColumnType.base[Form.Kind, Byte](
    {
      case Form.Kind.Active => 0
      case Form.Kind.Freezed => 1
    }, {
      case 0 => Form.Kind.Active
      case 1 => Form.Kind.Freezed
    }
  )

  /**
    * Form DB model.
    */
  case class DbForm(
    id: Long,
    name: String,
    kind: Form.Kind,
    isDeleted: Boolean,
    showInAggregation: Boolean,
    machineName: String
  ) {
    def toModel = this.transformInto[FormShort]
  }

  object DbForm {
    def fromModel(form: FormShort) =
      form
        .into[DbForm]
        .withFieldConst(_.isDeleted, false)
        .transform

  }

  class FormTable(tag: Tag) extends Table[DbForm](tag, "form") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def name = column[String]("name")
    def kind = column[Form.Kind]("kind")
    def isDeleted = column[Boolean]("is_deleted")
    def showInAggregation = column[Boolean]("show_in_aggregation")
    def machineName = column[String]("machine_name")

    def * = (id, name, kind, isDeleted, showInAggregation, machineName) <> ((DbForm.apply _).tupled, DbForm.unapply)
  }

  val Forms = TableQuery[FormTable]

  private val kindMapping: Map[Int, ElementKind] = {
    Map(
      0 -> TextField,
      1 -> TextArea,
      2 -> Checkbox,
      3 -> CheckboxGroup,
      4 -> Radio,
      5 -> Select,
      6 -> LikeDislike
    )
  }
  implicit lazy val kindColumnType = MappedColumnType.base[ElementKind, Byte](
    x => kindMapping.find(_._2 == x).get._1.toByte,
    kindMapping(_)
  )

  /**
    * Db model for form element.
    */
  case class DbFormElement(
    id: Long,
    formId: Long,
    kind: ElementKind,
    caption: String,
    required: Boolean,
    order: Int
  ) {

    def toModel(values: Seq[Form.ElementValue]) =
      this
        .into[Form.Element]
        .withFieldConst(_.values, values)
        .transform
  }

  object DbFormElement {

    def fromModel(formId: Long, element: Form.Element, order: Int): DbFormElement =
      element
        .into[DbFormElement]
        .withFieldConst(_.formId, formId)
        .withFieldConst(_.order, order)
        .transform
  }

  class FormElementTable(tag: Tag) extends Table[DbFormElement](tag, "form_element") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def formId = column[Long]("form_id")
    def kind = column[ElementKind]("kind")
    def caption = column[String]("caption")
    def required = column[Boolean]("required")
    def order = column[Int]("ord")

    def * =
      (id, formId, kind, caption, required, order) <>
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
  * Form DAO.
  */
@Singleton
class FormDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  implicit val ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
  with FormComponent
  with ProjectRelationComponent
  with EventProjectComponent
  with DaoHelper {

  import profile.api._

  /**
    * Returns list of forms.
    *
    * @param meta sorting and pagination
    */
  def getList(
    optKind: Option[Form.Kind] = None,
    optEventId: Option[Long] = None,
    includeDeleted: Boolean = false
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[FormShort]] = {

    def deletedFilter(form: FormTable) = if (includeDeleted) None else Some(!form.isDeleted)

    def eventFilter(form: FormTable) = optEventId.map { eventId =>
      val projectIds = EventProjects.filter(_.eventId === eventId).map(_.projectId)
      val formIds = Relations.filter(_.projectId.in(projectIds)).map(_.formId)
      form.id.in(formIds)
    }

    val query = Forms
      .applyFilter { form =>
        Seq(
          optKind.map(form.kind === _),
          deletedFilter(form),
          eventFilter(form)
        )
      }

    runListQuery(query) { form =>
      {
        case 'id => form.id
        case 'name => form.name
      }
    }.map { case ListWithTotal(total, data) => ListWithTotal(total, data.map(_.toModel)) }
  }

  /**
    * Creates form.
    *
    * @param form form model
    * @return created form model with ID
    */
  def create(form: FormShort): Future[FormShort] = {
    db.run(Forms.returning(Forms.map(_.id)).into((item, id) => item.copy(id = id).toModel) += DbForm.fromModel(form))
  }

  /**
    * Returns form with elements by ID.
    */
  def findById(id: Long): Future[Option[Form]] = {
    val query = Forms
      .filter(form => form.id === id && !form.isDeleted)
      .joinLeft {
        FormElements
          .joinLeft(FormElementValues)
          .on {
            case (element, value) => element.id === value.elementId
          }
      }
      .on {
        case (form, (element, _)) => form.id === element.formId
      }

    db.run(query.result).map { flatResults =>
      flatResults.headOption.map {
        case (form, _) =>
          val elements = flatResults
            .collect { case (_, Some(elementWithValues)) => elementWithValues }
            .groupBy { case (element, _) => element }
            .toSeq
            .sortBy { case (element, _) => element.order }
            .map {
              case (element, valuesWithElements) =>
                val values = valuesWithElements
                  .collect { case (_, Some(value)) => value }
                  .sortBy(_.order)
                  .map(_.toModel)

                element.toModel(values)
            }
          form.toModel.withElements(elements)
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

    val dbModels = elements.zipWithIndex
      .map {
        case (element, index) =>
          val values = element.values.zipWithIndex
            .map {
              case (value, valueIndex) =>
                DbFormElementValue(id = 0, elementId = 0, value.caption, valueIndex)
            }

          (DbFormElement.fromModel(formId, element, index), values)
      }

    val results = dbModels.map {
      case (element, values) =>
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
    } yield form.getOrElse(throw new NoSuchElementException("form not found")).elements
  }

  /**
    * Deletes form template with elements.
    *
    * @param id form template ID
    * @return number of rows affected
    */
  def delete(id: Long): Future[Int] = db.run {
    Forms.filter(_.id === id).map(_.isDeleted).update(true)
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
  def update(form: FormShort): Future[FormShort] =
    db.run {
        Forms.filter(_.id === form.id).update(DbForm.fromModel(form))
      }
      .map(_ => form)
}
