package services

import javax.inject.{Inject, Singleton}

import models.ListWithTotal
import models.dao.FormDao
import models.form.{Form, FormShort}
import models.user.User
import utils.errors.{ApplicationError, ConflictError, ExceptionHandler, NotFoundError}
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._

/**
  * Form template service.
  */
@Singleton
class FormService @Inject()(
  protected val formDao: FormDao
) extends ServiceResults[Form] {


  /**
    * Returns list of form templates without elements.
    *
    * @param account logged in user
    * @param meta    list meta
    */
  def getList()(implicit account: User, meta: ListMeta): EitherT[Future, ApplicationError, ListWithTotal[FormShort]] = {
    formDao.getList().lift
  }

  /**
    * Returns form template with elements or not found error.
    *
    * @param id      form ID
    * @param account logged in user
    */
  def getById(id: Long)(implicit account: User): SingleResult = {
    formDao.findById(id).liftRight {
      NotFoundError.Form(id)
    }
  }

  /**
    * Creates form and elements in DB.
    *
    * @param form    form model
    * @param account logged in user
    * @return created form
    */
  def create(form: Form)(implicit account: User): SingleResult = {
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
    for {
      _ <- getById(form.id)
      elements <- validateElements(form.elements).lift
      _ <- formDao.update(form.toShort).lift
      _ <- formDao.deleteElements(form.id).lift
      createdElements <- formDao.createElements(form.id, elements).lift
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
      _ <- getById(id)
      _ <- formDao.delete(id).lift(ExceptionHandler.sql)
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
