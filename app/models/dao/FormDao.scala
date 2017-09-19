package models.dao

import javax.inject.{Inject, Singleton}

import io.scalaland.chimney.dsl._
import models.ListWithTotal
import models.form.element._
import models.form.{Form, FormShort}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import utils.listmeta.ListMeta

import scala.concurrent.{ExecutionContext, Future}

trait FormComponent extends EnumColumnMapper { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  implicit lazy val formKindColumnType = mappedEnumSeq[Form.Kind](Form.Kind.Active, Form.Kind.Freezed)

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

  implicit lazy val kindColumnType = mappedEnumSeq[ElementKind](
    TextField,
    TextArea,
    Checkbox,
    CheckboxGroup,
    Radio,
    Select,
    LikeDislike,
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
    order: Int,
    machineName: String
  ) {

    def toModel(values: Seq[Form.ElementValue], competencies: Seq[Form.ElementCompetence]) =
      this
        .into[Form.Element]
        .withFieldConst(_.values, values)
        .withFieldConst(_.competencies, competencies)
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
    def machineName = column[String]("machine_name")

    def * =
      (id, formId, kind, caption, required, order, machineName) <>
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
    order: Int,
    competenceWeight: Option[Double]
  ) {

    def toModel = Form.ElementValue(
      id,
      caption,
      competenceWeight
    )
  }

  class FormElementValueTable(tag: Tag) extends Table[DbFormElementValue](tag, "form_element_value") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def elementId = column[Long]("element_id")
    def caption = column[String]("caption")
    def order = column[Int]("ord")
    def competenceWeight = column[Option[Double]]("competence_weight")

    def * =
      (id, elementId, caption, order, competenceWeight) <> ((DbFormElementValue.apply _).tupled, DbFormElementValue.unapply)
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
  with FormElementCompetenceComponent
  with CompetenceComponent
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
    */
  def create(form: Form): Future[Form] = {
    val createForm = {
      Forms.returning(Forms.map(_.id)) += DbForm.fromModel(form.toShort)
    }

    val actions = for {
      formId <- createForm
      _ <- createElementsIO(formId, form.elements)
    } yield formId

    for {
      id <- db.run(actions.transactionally)
      formById <- findById(id)
    } yield formById.getOrElse(form.copy(id = id))
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
          .joinLeft {
            FormElementCompetencies
              .join(Competencies)
              .on(_.competenceId === _.id)
          }
          .on {
            case ((element, _), (competence, _)) => element.id === competence.elementId
          }
      }
      .on {
        case (form, ((element, _), _)) => form.id === element.formId
      }

    db.run(query.result).map { flatResults =>
      flatResults.headOption.map {
        case (form, _) =>
          val elements = flatResults
            .collect { case (_, Some(elementWithValues)) => elementWithValues }
            .groupBy { case ((element, _), _) => element }
            .toSeq
            .sortBy { case (element, _) => element.order }
            .map {
              case (element, flatResult) =>
                val values = flatResult
                  .collect { case ((_, Some(value)), _) => value }
                  .distinct
                  .sortBy(_.order)
                  .map(_.toModel)
                val competencies = flatResult
                  .collect { case (_, Some(competence)) => competence }
                  .distinct
                  .map {
                    case (elementCompetence, competence) => {
                      Form.ElementCompetence(
                        competence.toNamedEntity,
                        elementCompetence.factor
                      )
                    }
                  }

                element.toModel(values, competencies)
            }
          form.toModel.withElements(elements)
      }
    }
  }

  /**
    * DBIO for creating elements.
    */
  private def createElementsIO(formId: Long, elements: Seq[Form.Element]) = {
    def createValue(elId: Long, value: Form.ElementValue, order: Int) = {
      FormElementValues += DbFormElementValue(0, elId, value.caption, order, value.competenceWeight)
    }

    def createValues(elId: Long, values: Seq[Form.ElementValue]) = {
      DBIO.sequence {
        values.zipWithIndex.map {
          case (value, index) =>
            createValue(elId, value, index)
        }
      }
    }

    def createCompetence(elId: Long, elC: Form.ElementCompetence) = {
      FormElementCompetencies += DbFormElementCompetence(elId, elC.competence.id, elC.factor)
    }

    def createCompetencies(elId: Long, c: Seq[Form.ElementCompetence]) = {
      DBIO.sequence(c.map(createCompetence(elId, _)))
    }

    def createElement(el: Form.Element, order: Int) = {
      for {
        elId <- FormElements.returning(FormElements.map(_.id)) += DbFormElement.fromModel(formId, el, order)
        _ <- createValues(elId, el.values)
        _ <- createCompetencies(elId, el.competencies)
      } yield ()
    }

    val createElements = DBIO
      .sequence {
        elements.zipWithIndex.map {
          case (element, index) =>
            createElement(element, index)
        }
      }

    createElements.map(_ => ())
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
    * Updates form.
    */
  def update(form: Form): Future[Form] = {
    val actions = for {
      _ <- Forms.filter(_.id === form.id).update(DbForm.fromModel(form.toShort))
      _ <- FormElements.filter(_.formId === form.id).delete
      _ <- createElementsIO(form.id, form.elements)
    } yield ()

    for {
      _ <- db.run(actions.transactionally)
      formById <- findById(form.id)
    } yield formById.getOrElse(form)
  }
}
