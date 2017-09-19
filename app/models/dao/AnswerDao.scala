package models.dao

import javax.inject.{Inject, Singleton}

import models.NamedEntity
import models.assessment.Answer
import org.davidbild.tristate.Tristate
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Component for form_answer, form_element_answer and form_element_answer_value tables.
  */
trait AnswerComponent extends EnumColumnMapper { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  implicit lazy val answerStatusColumnType = mappedEnumSeq[Answer.Status](
    Answer.Status.New,
    Answer.Status.Answered,
    Answer.Status.Skipped,
  )

  /**
    * Form answer DB model.
    */
  case class DbAnswer(
    id: Long,
    activeProjectId: Long,
    userFromId: Long,
    userToId: Option[Long],
    formId: Long,
    isAnonymous: Boolean,
    status: Answer.Status,
    canSkip: Boolean
  ) {
    def toModel(answers: Seq[Answer.Element], formName: String) =
      this
        .into[Answer]
        .withFieldComputed(_.form, x => NamedEntity(x.formId, formName))
        .withFieldConst(_.elements, answers.toSet)
        .transform
  }

  object DbAnswer {
    def fromModel(answer: Answer): DbAnswer =
      answer
        .into[DbAnswer]
        .withFieldConst(_.id, 0L)
        .withFieldComputed(_.formId, _.form.id)
        .transform
  }

  class AnswerTable(tag: Tag) extends Table[DbAnswer](tag, "form_answer") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def activeProjectId = column[Long]("active_project_id")
    def userFromId = column[Long]("user_from_id")
    def userToId = column[Option[Long]]("user_to_id")
    def formId = column[Long]("form_id")
    def isAnonymous = column[Boolean]("is_anonymous")
    def status = column[Answer.Status]("status")
    def canSkip = column[Boolean]("can_skip")

    def * =
      (id, activeProjectId, userFromId, userToId, formId, isAnonymous, status, canSkip) <> ((DbAnswer.apply _).tupled, DbAnswer.unapply)
  }

  val Answers = TableQuery[AnswerTable]

  /**
    * Element answer DB model.
    */
  case class DbFormElementAnswer(
    id: Long,
    answerId: Long,
    elementId: Long,
    text: Option[String],
    comment: Option[String]
  ) {
    def toModel(values: Option[Seq[Long]]) =
      this
        .into[Answer.Element]
        .withFieldConst(_.valuesIds, values.map(_.toSet))
        .transform

  }

  object DbFormElementAnswer {
    def fromModel(el: Answer.Element, answerId: Long) =
      el.into[DbFormElementAnswer]
        .withFieldConst(_.id, 0L)
        .withFieldConst(_.answerId, answerId)
        .transform

  }

  class FormElementAnswerTable(tag: Tag) extends Table[DbFormElementAnswer](tag, "form_element_answer") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def answerId = column[Long]("answer_id")
    def formElementId = column[Long]("form_element_id")
    def text = column[Option[String]]("text")
    def comment = column[Option[String]]("comment")

    def * =
      (id, answerId, formElementId, text, comment) <> ((DbFormElementAnswer.apply _).tupled, DbFormElementAnswer.unapply)
  }

  val FormElementAnswers = TableQuery[FormElementAnswerTable]

  /**
    * Element value DB model.
    */
  case class DbFormElementAnswerValue(
    id: Long,
    answerElementId: Long,
    valueId: Long
  )

  class FormElementAnswerValueTable(tag: Tag)
    extends Table[DbFormElementAnswerValue](tag, "form_element_answer_value") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def answerElementId = column[Long]("answer_element_id")
    def formElementValueId = column[Long]("form_element_value_id")

    def * =
      (id, answerElementId, formElementValueId) <> ((DbFormElementAnswerValue.apply _).tupled, DbFormElementAnswerValue.unapply)
  }

  val FormElementAnswerValues = TableQuery[FormElementAnswerValueTable]
}

/**
  * Answer DAO.
  */
@Singleton
class AnswerDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  implicit val ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
  with AnswerComponent
  with FormComponent
  with DaoHelper
  with ActiveProjectComponent {

  import profile.api._

  private def userToFilter(answer: AnswerTable, userToId: Option[Long]) = userToId match {
    case Some(toId) => answer.userToId.fold(false: Rep[Boolean])(_ === toId)
    case None       => answer.userToId.isEmpty
  }

  /**
    * Returns list of answers by given criteria.
    */
  def getList(
    optEventId: Option[Long] = None,
    optActiveProjectId: Option[Long] = None,
    optUserFromId: Option[Long] = None,
    optFormId: Option[Long] = None,
    optUserToId: Tristate[Long] = Tristate.Unspecified
  ): Future[Seq[Answer]] = {

    def userToFilter(answer: AnswerTable) = optUserToId match {
      case Tristate.Unspecified       => None
      case Tristate.Absent            => Some(answer.userToId.isEmpty)
      case Tristate.Present(userToId) => Some(answer.userToId.fold(false: Rep[Boolean])(_ === userToId))
    }

    val query = Answers
      .applyFilter(answer =>
        Seq(
          optEventId.map(eventId => answer.activeProjectId.in(ActiveProjects.filter(_.eventId === eventId).map(_.id))),
          optActiveProjectId.map(answer.activeProjectId === _),
          optUserFromId.map(answer.userFromId === _),
          optFormId.map(answer.formId === _),
          userToFilter(answer)
      ))
      .join(Forms)
      .on(_.formId === _.id)
      .joinLeft {
        FormElementAnswers
          .joinLeft(FormElementAnswerValues)
          .on(_.id === _.answerElementId)
      }
      .on { case ((answer, _), (element, _)) => answer.id === element.answerId }

    db.run(query.result).map { flatResults =>
      flatResults
        .groupBy {
          case ((answer, form), _) =>
            (answer, form)
        }
        .map {
          case ((answer, form), abc) =>
            val elements = abc
              .collect { case (_, Some(elementWithValues)) => elementWithValues }
              .groupBy { case (element, _) => element }
              .map {
                case (element, elementWithValues) =>
                  val values = elementWithValues
                    .collect { case (_, Some(value)) => value }
                    .map(_.valueId)

                  val valuesOpt = if (values.isEmpty) None else Some(values)
                  element.toModel(valuesOpt)
              }
              .toSeq
            answer.toModel(elements, form.name)
        }
        .toSeq
    }
  }

  /**
    * Returns answer by given criteria.
    */
  def getAnswer(
    activeProjectId: Long,
    userFromId: Long,
    userToId: Option[Long],
    formId: Long
  ): Future[Option[Answer]] =
    getList(
      optActiveProjectId = Some(activeProjectId),
      optUserFromId = Some(userFromId),
      optUserToId = Tristate.fromOption(userToId),
      optFormId = Some(formId)
    ).map(_.headOption)

  /**
    * Saves answer with elements and values in DB.
    */
  def saveAnswer(answer: Answer): Future[Answer] = {
    val deleteExistedAnswer = Answers.filter { x =>
      x.activeProjectId === answer.activeProjectId &&
      x.userFromId === answer.userFromId &&
      userToFilter(x, answer.userToId) &&
      x.formId === answer.form.id
    }.delete

    def insertAnswerElementAction(answerId: Long, element: Answer.Element) = {
      for {
        elementId <- FormElementAnswers.returning(FormElementAnswers.map(_.id)) +=
          DbFormElementAnswer.fromModel(element, answerId)

        _ <- FormElementAnswerValues ++=
          element.valuesIds.getOrElse(Nil).map(DbFormElementAnswerValue(0, elementId, _))
      } yield ()
    }

    val actions = for {
      _ <- deleteExistedAnswer
      answerId <- Answers.returning(Answers.map(_.id)) +=
        DbAnswer.fromModel(answer)

      _ <- DBIO.seq(answer.elements.toSeq.map(insertAnswerElementAction(answerId, _)): _*)
    } yield ()

    db.run(actions.transactionally).map(_ => answer)
  }

  def createAnswer(answer: Answer): Future[Unit] = {
    db.run(Answers += DbAnswer.fromModel(answer)).map(_ => ())
  }
}
