package models.dao

import javax.inject.{Inject, Singleton}

import models.NamedEntity
import models.assessment.Answer
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
  * Component for form_answer, form_element_answer and form_element_answer_value tables.
  */
trait AnswerComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  /**
    * Form answer DB model.
    */
  case class DbFormAnswer(
    id: Long,
    eventId: Long,
    projectId: Option[Long],
    userFromId: Long,
    userToId: Option[Long],
    formId: Long
  ) {
    def toModel(answers: Seq[Answer.Element], formName: String) = Answer.Form(
      NamedEntity(formId, formName),
      answers.toSet
    )
  }

  class FormAnswerTable(tag: Tag) extends Table[DbFormAnswer](tag, "form_answer") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def eventId = column[Long]("event_id")
    def projectId = column[Option[Long]]("project_id")
    def userFromId = column[Long]("user_from_id")
    def userToId = column[Option[Long]]("user_to_id")
    def formId = column[Long]("form_id")

    def * = (id, eventId, projectId, userFromId, userToId, formId) <> ((DbFormAnswer.apply _).tupled, DbFormAnswer.unapply)
  }

  val FormAnswers = TableQuery[FormAnswerTable]

  /**
    * Element answer DB model.
    */
  case class DbFormElementAnswer(
    id: Long,
    answerId: Long,
    elementId: Long,
    text: Option[String]
  ) {
    def toModel(values: Option[Seq[Long]]) = Answer.Element(
      elementId,
      text,
      values
    )
  }

  class FormElementAnswerTable(tag: Tag) extends Table[DbFormElementAnswer](tag, "form_element_answer") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def answerId = column[Long]("answer_id")
    def formElementId = column[Long]("form_element_id")
    def text = column[Option[String]]("text")

    def * = (id, answerId, formElementId, text) <> ((DbFormElementAnswer.apply _).tupled, DbFormElementAnswer.unapply)
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

  class FormElementAnswerValueTable(tag: Tag) extends Table[DbFormElementAnswerValue](tag, "form_element_answer_value") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def answerElementId = column[Long]("answer_element_id")
    def formElementValueId = column[Long]("form_element_value_id")

    def * = (id, answerElementId, formElementValueId) <> ((DbFormElementAnswerValue.apply _).tupled, DbFormElementAnswerValue.unapply)
  }

  val FormElementAnswerValues = TableQuery[FormElementAnswerValueTable]
}

/**
  * Answer DAO.
  */
@Singleton
class AnswerDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
  with AnswerComponent
  with FormComponent
  with DaoHelper {

  import driver.api._

  private def userToFilter(answer: FormAnswerTable, userToId: Option[Long]) = userToId match {
    case Some(toId) => answer.userToId.fold(false: Rep[Boolean])(_ === toId)
    case None => answer.userToId.isEmpty
  }

  /**
    * Returns answer by given criteria.
    */
  def getAnswer(
    eventId: Long,
    projectId: Long,
    fromUserId: Long,
    toUserId: Option[Long],
    formId: Long
  ): Future[Option[Answer.Form]] = {

    val query = FormAnswers.filter { answer =>
        answer.eventId === eventId &&
        answer.projectId === projectId &&
        answer.userFromId === fromUserId &&
        userToFilter(answer, toUserId) &&
        answer.formId === formId
      }
      .join(Forms).on(_.formId === _.id)
      .take(1)
      .joinLeft {
        FormElementAnswers
          .joinLeft(FormElementAnswerValues)
          .on(_.id === _.answerElementId)
      }.on { case ((answer, _), (element, _)) => answer.id === element.answerId }

    db.run(query.result).map { flatResults =>
      flatResults.headOption.map { case ((answer, form), _) =>
        val elements = flatResults
          .collect { case (_, Some(elementWithValues)) => elementWithValues }
          .groupBy { case (element, _) => element }
          .map { case (element, elementWithValues) =>
            val values = elementWithValues
              .collect { case (_, Some(value)) => value }
              .map(_.valueId)

            val valuesOpt = if (values.isEmpty) None else Some(values)
            element.toModel(valuesOpt)
          }
          .toSeq
        answer.toModel(elements, form.name)
      }
    }
  }

  /**
    * Saves answer with elements and values in DB.
    */
  def saveAnswer(
    eventId: Long,
    projectId: Long,
    fromUserId: Long,
    toUserId: Option[Long],
    answer: Answer.Form
  ): Future[Answer.Form] = {
    val deleteExistedAnswer = FormAnswers.filter { x =>
      x.eventId === eventId &&
        x.projectId === projectId &&
        x.userFromId === fromUserId &&
        userToFilter(x, toUserId) &&
        x.formId === answer.form.id
    }.delete

    def insertAnswerElementAction(answerId: Long, element: Answer.Element) = {
      for {
        elementId <- FormElementAnswers.returning(FormElementAnswers.map(_.id)) +=
          DbFormElementAnswer(0, answerId, element.elementId, element.text)

        _ <- FormElementAnswerValues ++=
          element.valuesIds.getOrElse(Nil).map(DbFormElementAnswerValue(0, elementId, _))
      } yield ()
    }

    val actions = for {
      _ <- deleteExistedAnswer
      answerId <- FormAnswers.returning(FormAnswers.map(_.id)) +=
        DbFormAnswer(0, eventId, Some(projectId), fromUserId, toUserId, answer.form.id)

      _ <- DBIO.seq(answer.answers.toSeq.map(insertAnswerElementAction(answerId, _)): _*)
    } yield ()

    db.run(actions.transactionally).map(_ => answer)
  }
}
