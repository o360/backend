package services

import javax.inject.{Inject, Singleton}

import models.ListWithTotal
import models.dao.{EventDao, FormDao}
import models.event.Event
import models.form.{Form, FormShort}
import models.user.User
import utils.errors.{ApplicationError, ConflictError, ExceptionHandler, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._
import play.api.libs.concurrent.Execution.Implicits._

/**
  * Form template service.
  */
@Singleton
class FormService @Inject()(
  protected val formDao: FormDao,
  protected val eventDao: EventDao
) extends ServiceResults[Form] {


  /**
    * Returns list of form templates without elements.
    *
    * @param account logged in user
    * @param meta    list meta
    */
  def getList()(implicit account: User, meta: ListMeta): EitherT[Future, ApplicationError, ListWithTotal[FormShort]] = {
    formDao.getList(optKind = Some(Form.Kind.Active)).lift
  }

  /**
    * Returns form template with elements or not found error.
    *
    * @param id      form ID
    */
  def getById(id: Long): SingleResult = {
    formDao.findById(id).liftRight {
      NotFoundError.Form(id)
    }
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
      createdForm <- formDao.create(form.toShort).lift
      createdElements <- formDao.createElements(createdForm.id, elements).lift
    } yield createdForm.toModel(createdElements)
  }

  /**
    * Updates form and elements in DB.
    *
    * @param form    form
    * @param account logged in user
    * @return updated form
    */
  def update(form: Form)(implicit account: User): SingleResult = {

    def updateChildFreezedForms() = for {
      childFreezedForms <- formDao.getList(optKind = Some(Form.Kind.Freezed), optFormTemplateId = Some(form.id))

      _ <- Future.sequence {
        childFreezedForms.data.map { childFreezedForm =>
          formDao.update(childFreezedForm.copy(name = form.name))
        }
      }
    } yield ()

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
      _ <- formDao.update(form.toShort).lift
      _ <- formDao.deleteElements(form.id).lift
      createdElements <- formDao.createElements(form.id, elements).lift

      _ <- updateChildFreezedForms().lift
    } yield form.copy(elements = createdElements)
  }

  /**
    * Deletes form with elements.
    *
    * @param id      form ID
    * @param account logged in user
    */
  def delete(id: Long)(implicit account: User): UnitResult = {
    for {
      original <- getById(id)
      _ <- ensure(original.kind != Form.Kind.Freezed) {
        ConflictError.Form.FormKind("delete freezed form")
      }
      _ <- formDao.delete(id).lift(ExceptionHandler.sql)
    } yield ()
  }

  /**
    * Creates freezed form if not exists and returns it.
    *
    * @param eventId event ID
    * @param formId  template form ID
    */
  def getOrCreateFreezedForm(eventId: Long, formId: Long): SingleResult = {
    for {
      freezedFormId <- formDao.getFreezedFormId(eventId, formId).lift
      freezedForm <- freezedFormId match {
        case Some(freezedFormId) => getById(freezedFormId)
        case None =>
          for {
            form <- getById(formId)
            freezedForm <- create(form.copy(kind = Form.Kind.Freezed))
            _ <- formDao.setFreezedFormId(eventId, formId, freezedForm.id).lift
          } yield freezedForm
      }
    } yield freezedForm
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

      if (needValues && isEmptyValues) {
        ConflictError.Form.ElementValuesMissed(s"${element.kind}: ${element.caption}").left
      } else if (needToRemoveValues) {
        element.copy(values = Nil).right
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
