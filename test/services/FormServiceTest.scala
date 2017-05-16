package services

import java.sql.Timestamp

import models.ListWithTotal
import models.dao.{EventDao, FormDao}
import models.event.Event
import models.form.{Form, FormShort}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import testutils.fixture.{FormFixture, UserFixture}
import testutils.generator.FormGenerator
import utils.errors.{ConflictError, NotFoundError}
import utils.listmeta.ListMeta

/**
  * Test for form service.
  */
class FormServiceTest extends BaseServiceTest with FormGenerator with FormFixture {

  private val admin = UserFixture.admin

  private case class TestFixture(
    formDaoMock: FormDao,
    eventDao: EventDao,
    service: FormService)

  private def getFixture = {
    val daoMock = mock[FormDao]
    val eventDao = mock[EventDao]
    val service = new FormService(daoMock, eventDao)
    TestFixture(daoMock, eventDao, service)
  }

  "getById" should {

    "return not found if form not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.formDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.getById(id)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.formDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.formDaoMock)
      }
    }

    "return form from db" in {
      forAll { (form: Form, id: Long) =>
        val fixture = getFixture
        when(fixture.formDaoMock.findById(id)).thenReturn(toFuture(Some(form)))
        val result = wait(fixture.service.getById(id)(admin).run)

        result mustBe 'right
        result.toOption.get mustBe form

        verify(fixture.formDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.formDaoMock)
      }
    }
  }

  "list" should {
    "return list of forms from db" in {
      forAll { (
      forms: Seq[FormShort],
      total: Int
      ) =>
        val fixture = getFixture
        when(fixture.formDaoMock.getList()(eqTo(ListMeta.default)))
          .thenReturn(toFuture(ListWithTotal(total, forms)))
        val result = wait(fixture.service.getList()(admin, ListMeta.default).run)

        result mustBe 'right
        result.toOption.get mustBe ListWithTotal(total, forms)

        verify(fixture.formDaoMock, times(1)).getList()(eqTo(ListMeta.default))
      }
    }
  }

  "create" should {
    "return conflict if form is incorrect" in {
      val form = Forms(0).copy(elements = Seq(Form.Element(1, Form.ElementKind.Radio, "", false, Nil)))

      val fixture = getFixture
      val result = wait(fixture.service.create(form)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "create form in db" in {
      val form = Forms(0)
      val fixture = getFixture
      when(fixture.formDaoMock.create(form.toShort)).thenReturn(toFuture(form.toShort))
      when(fixture.formDaoMock.createElements(eqTo(form.id), any[Seq[Form.Element]]))
        .thenReturn(toFuture(form.elements))

      val result = wait(fixture.service.create(form)(admin).run)

      result mustBe 'right
      result.toOption.get mustBe form
    }
  }

  "update" should {
    "return not found if form not found" in {
      forAll { (form: Form) =>
        val fixture = getFixture
        when(fixture.formDaoMock.findById(form.id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.update(form)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.formDaoMock, times(1)).findById(form.id)
        verifyNoMoreInteractions(fixture.formDaoMock)
      }
    }

    "return conflict if form is incorrect" in {
      val form = Forms(0).copy(elements = Seq(Form.Element(1, Form.ElementKind.Radio, "", false, Nil)))
      val fixture = getFixture
      when(fixture.formDaoMock.findById(form.id)).thenReturn(toFuture(Some(form)))
      when(fixture.eventDao.getList(
        optId = any[Option[Long]],
        optStatus = eqTo(Some(Event.Status.InProgress)),
        optProjectId = any[Option[Long]],
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = eqTo(Some(form.id))
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))

      val result = wait(fixture.service.update(form)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "update form in db" in {
      val form = Forms(0)
      val fixture = getFixture
      when(fixture.formDaoMock.findById(form.id)).thenReturn(toFuture(Some(form)))
      when(fixture.formDaoMock.update(form.toShort)).thenReturn(toFuture(form.toShort))
      when(fixture.formDaoMock.deleteElements(form.id)).thenReturn(toFuture(form.elements.length))
      when(fixture.formDaoMock.createElements(eqTo(form.id), any[Seq[Form.Element]]))
        .thenReturn(toFuture(form.elements))
      when(fixture.eventDao.getList(
        optId = any[Option[Long]],
        optStatus = eqTo(Some(Event.Status.InProgress)),
        optProjectId = any[Option[Long]],
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = eqTo(Some(form.id))
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))

      val result = wait(fixture.service.update(form)(admin).run)

      result mustBe 'right
      result.toOption.get mustBe form
    }
  }

  "delete" should {
    "return not found if form not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.formDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.delete(id)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.formDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.formDaoMock)
      }
    }

    "delete form from db" in {
      forAll { (form: Form) =>
        val fixture = getFixture
        when(fixture.formDaoMock.findById(form.id)).thenReturn(toFuture(Some(form)))
        when(fixture.formDaoMock.delete(form.id)).thenReturn(toFuture(1))

        val result = wait(fixture.service.delete(form.id)(admin).run)

        result mustBe 'right
      }
    }
  }
}
