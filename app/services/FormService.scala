package services

import javax.inject.{Inject, Singleton}

import models.ListWithTotal
import models.dao._
import models.event.Event
import models.form.{Form, FormShort}
import models.project.Project
import models.user.User
import utils.errors._
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._
import scalaz._

/**
  * Form template service.
  */
@Singleton
class FormService @Inject()(
  protected val formDao: FormDao,
  protected val eventDao: EventDao,
  protected val groupDao: GroupDao,
  protected val projectDao: ProjectDao,
  protected val relationDao: ProjectRelationDao,
  protected val answerDao: AnswerDao,
  implicit val ec: ExecutionContext
) extends ServiceResults[Form] {

  /**
    * Returns list of form templates without elements.
    *
    * @param meta    list meta
    */
  def getList()(implicit meta: ListMeta): EitherT[Future, ApplicationError, ListWithTotal[FormShort]] = {
    formDao.getList(optKind = Some(Form.Kind.Active)).lift
  }

  /**
    * Returns form template with elements or not found error.
    *
    * @param id      form ID
    */
  def getById(id: Long): SingleResult = {
    for {
      form <- formDao.findById(id).liftRight {
        NotFoundError.Form(id)
      }
    } yield form
  }

  /**
    * Returns form by ID for user.
    */
  def getByIdWithAuth(id: Long)(implicit account: User): SingleResult = {
    for {
      form <- getById(id)
      answers <- answerDao.getList(optUserFromId = Some(account.id), optFormId = Some(id)).lift
      _ <- ensure(answers.nonEmpty) {
        NotFoundError.Form(id)
      }
    } yield form
  }

  /**
    * Creates form and elements in DB.
    *
    * @param form    form model
    * @return created form
    */
  def create(form: Form): SingleResult = {
    for {
      elements <- validateElements(form.elements).lift
      createdForm <- formDao.create(form.copy(elements = elements)).lift(ExceptionHandler.sql)
    } yield createdForm
  }

  /**
    * Updates form and elements in DB.
    *
    * @param form    form
    * @return updated form
    */
  def update(form: Form): SingleResult = {

    for {
      original <- getById(form.id)

      _ <- ensure(original.kind == form.kind) {
        ConflictError.Form.FormKind("change form kind")
      }

      activeEvents <- eventDao.getList(optStatus = Some(Event.Status.InProgress), optFormId = Some(form.id)).lift
      _ <- ensure(activeEvents.total == 0) {
        ConflictError.Form.ActiveEventExists
      }

      elements <- validateElements(form.elements).lift
      updatedForm <- formDao.update(form.copy(elements = elements)).lift(ExceptionHandler.sql)

    } yield updatedForm
  }

  /**
    * Deletes form with elements.
    *
    * @param id      form ID
    */
  def delete(id: Long): UnitResult = {

    def getConflictedEntities = {
      for {
        projects <- projectDao.getList(optFormId = Some(id))
        relations <- relationDao.getList(optFormId = Some(id))
      } yield {
        ConflictError.getConflictedEntitiesMap(
          Project.namePlural -> (projects.data.map(_.toNamedEntity) ++ relations.data.map(_.project)).distinct
        )
      }
    }

    for {
      original <- getById(id)
      _ <- ensure(original.kind != Form.Kind.Freezed) {
        ConflictError.Form.FormKind("delete freezed form")
      }

      conflictedEntities <- getConflictedEntities.lift
      _ <- ensure(conflictedEntities.isEmpty) {
        ConflictError.General(Some(Form.nameSingular), conflictedEntities)
      }

      _ <- formDao.delete(id).lift
    } yield ()
  }

  /**
    * Validates form elements and returns either new form elements or error.
    *
    * @param elements form elements
    */
  private def validateElements(elements: Seq[Form.Element]): ApplicationError \/ Seq[Form.Element] = {
    def validateElement(element: Form.Element): ApplicationError \/ Form.Element = {
      val needValues = element.kind.needValues
      val isEmptyValues = element.values.isEmpty
      val needToRemoveValues = !needValues && !isEmptyValues
      val validLikeDislike = element.kind != models.form.element.LikeDislike ||
        element.values.map(_.caption).toSet == Set("like", "dislike")

      if (needValues && isEmptyValues) {
        ConflictError.Form.ElementValuesMissed(s"${element.kind}: ${element.caption}").left
      } else if (needToRemoveValues) {
        element.copy(values = Nil).right
      } else if (!validLikeDislike) {
        ConflictError.Form.MissedValuesInLikeDislike.left
      } else {
        element.right
      }
    }

    elements
      .foldLeft(Seq.empty[Form.Element].right[ApplicationError]) { (result, sourceElement) =>
        for {
          elements <- result
          validatedElement <- validateElement(sourceElement)
        } yield elements :+ validatedElement
      }
  }
}
