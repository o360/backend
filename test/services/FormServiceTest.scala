package services

import models.ListWithTotal
import models.dao._
import models.event.Event
import models.form.{Form, FormShort}
import models.project.{Project, Relation}
import org.mockito.Mockito._
import testutils.fixture._
import testutils.generator.FormGenerator
import utils.errors.{ConflictError, NotFoundError}
import utils.listmeta.ListMeta

/**
  * Test for form service.
  */
class FormServiceTest
  extends BaseServiceTest
  with FormGenerator
  with FormFixture
  with EventFixture
  with ProjectFixture
  with AnswerFixture {

  private val admin = UserFixture.admin

  private case class TestFixture(
    formDaoMock: FormDao,
    eventDao: EventDao,
    groupDao: GroupDao,
    projectDao: ProjectDao,
    relationDao: ProjectRelationDao,
    answerDao: AnswerDao,
    service: FormService
  )

  private def getFixture = {
    val daoMock = mock[FormDao]
    val eventDao = mock[EventDao]
    val groupDao = mock[GroupDao]
    val projectDao = mock[ProjectDao]
    val relationDao = mock[ProjectRelationDao]
    val answerDao = mock[AnswerDao]
    val service = new FormService(daoMock, eventDao, groupDao, projectDao, relationDao, answerDao, ec)
    TestFixture(daoMock, eventDao, groupDao, projectDao, relationDao, answerDao, service)
  }

  "getById" should {

    "return not found if form not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.formDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.getById(id).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.formDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.formDaoMock)
      }
    }

    "return form from db" in {
      forAll { (form: Form, id: Long) =>
        val fixture = getFixture
        when(fixture.formDaoMock.findById(id)).thenReturn(toFuture(Some(form)))
        val result = wait(fixture.service.getById(id).run)

        result mustBe right
        result.toOption.get mustBe form

        verify(fixture.formDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.formDaoMock)
      }
    }
  }

  "userGetById" should {
    "return error if can't get form" in {
      val fixture = getFixture
      val formId = 1
      when(fixture.formDaoMock.findById(formId)).thenReturn(toFuture(Some(Forms(0))))
      when(
        fixture.answerDao.getList(
          optEventId = *,
          optActiveProjectId = *,
          optUserFromId = eqTo(Some(admin.id)),
          optFormId = eqTo(Some(formId)),
          optUserToId = *
        )
      ).thenReturn(toFuture(Nil))

      val result = wait(fixture.service.getByIdWithAuth(formId)(admin).run)

      result mustBe left
      result.swap.toOption.get mustBe an[NotFoundError]
    }

    "return form" in {
      val fixture = getFixture
      val formId = 1
      when(fixture.formDaoMock.findById(formId)).thenReturn(toFuture(Some(Forms(0))))
      when(
        fixture.answerDao.getList(
          optEventId = *,
          optActiveProjectId = *,
          optUserFromId = eqTo(Some(admin.id)),
          optFormId = eqTo(Some(formId)),
          optUserToId = *
        )
      ).thenReturn(toFuture(Seq(Answers(0))))

      val result = wait(fixture.service.getByIdWithAuth(formId)(admin).run)

      result mustBe right
      result.toOption.get mustBe Forms(0)
    }
  }

  "list" should {
    "return list of forms from db" in {
      forAll {
        (
          forms: Seq[FormShort],
          total: Int
        ) =>
          val fixture = getFixture
          when(
            fixture.formDaoMock.getList(
              optKind = *,
              optEventId = *,
              includeDeleted = *
            )(eqTo(ListMeta.default))
          ).thenReturn(toFuture(ListWithTotal(total, forms)))
          val result = wait(fixture.service.getList()(ListMeta.default).run)

          result mustBe right
          result.toOption.get mustBe ListWithTotal(total, forms)
      }
    }
  }

  "create" should {
    "return conflict if form is incorrect" in {
      val form =
        Forms(0).copy(elements = Seq(Form.Element(1, models.form.element.Radio, "", false, Nil, Nil, "", None)))

      val fixture = getFixture
      val result = wait(fixture.service.create(form).run)

      result mustBe left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "create form in db" in {
      val form = Forms(0)
      val fixture = getFixture
      when(fixture.formDaoMock.create(form)).thenReturn(toFuture(form))

      val result = wait(fixture.service.create(form).run)

      result mustBe right
      result.toOption.get mustBe form
    }
  }

  "update" should {
    "return not found if form not found" in {
      forAll { (form: Form) =>
        val fixture = getFixture
        when(fixture.formDaoMock.findById(form.id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.update(form).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.formDaoMock, times(1)).findById(form.id)
        verifyNoMoreInteractions(fixture.formDaoMock)
      }
    }

    "return conflict if form is incorrect" in {
      val form =
        Forms(0).copy(elements = Seq(Form.Element(1, models.form.element.Radio, "", false, Nil, Nil, "", None)))
      val fixture = getFixture
      when(fixture.formDaoMock.findById(form.id)).thenReturn(toFuture(Some(form)))
      when(
        fixture.eventDao.getList(
          optId = *,
          optStatus = eqTo(Some(Event.Status.InProgress)),
          optProjectId = *,
          optFormId = eqTo(Some(form.id)),
          optGroupFromIds = *,
          optUserId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))

      val result = wait(fixture.service.update(form).run)

      result mustBe left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "update form in db" in {
      val form = Forms(0)
      val fixture = getFixture
      when(fixture.formDaoMock.findById(form.id)).thenReturn(toFuture(Some(form)))
      when(fixture.formDaoMock.update(form)).thenReturn(toFuture(form))
      when(
        fixture.eventDao.getList(
          optId = *,
          optStatus = eqTo(Some(Event.Status.InProgress)),
          optProjectId = *,
          optFormId = eqTo(Some(form.id)),
          optGroupFromIds = *,
          optUserId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))

      val result = wait(fixture.service.update(form).run)

      result mustBe right
      result.toOption.get mustBe form
    }
  }

  "delete" should {
    "return not found if form not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.formDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.delete(id).run)

        result mustBe left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.formDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.formDaoMock)
      }
    }

    "return conflict if form is freezed" in {
      forAll { (form: Form) =>
        val fixture = getFixture
        when(fixture.formDaoMock.findById(form.id)).thenReturn(toFuture(Some(form.copy(kind = Form.Kind.Freezed))))

        val result = wait(fixture.service.delete(form.id).run)

        result mustBe left
        result.swap.toOption.get mustBe a[ConflictError]
      }
    }

    "return conflict if relations exist" in {
      forAll { (form: Form) =>
        val fixture = getFixture
        when(fixture.formDaoMock.findById(form.id)).thenReturn(toFuture(Some(form.copy(kind = Form.Kind.Active))))
        when(
          fixture.projectDao.getList(
            optId = *,
            optEventId = *,
            optGroupFromIds = *,
            optFormId = eqTo(Some(form.id)),
            optGroupAuditorId = *,
            optEmailTemplateId = *,
            optAnyRelatedGroupId = *
          )(*)
        ).thenReturn(toFuture(ListWithTotal(1, Projects.take(1))))
        when(
          fixture.relationDao.getList(
            optId = *,
            optProjectId = *,
            optKind = *,
            optFormId = eqTo(Some(form.id)),
            optGroupFromId = *,
            optGroupToId = *,
            optEmailTemplateId = *
          )(*)
        ).thenReturn(toFuture(ListWithTotal[Relation](0, Nil)))

        val result = wait(fixture.service.delete(form.id).run)

        result mustBe left
        result.swap.toOption.get mustBe a[ConflictError]
      }
    }

    "delete form from db" in {
      forAll { (form: Form) =>
        val fixture = getFixture
        when(fixture.formDaoMock.findById(form.id)).thenReturn(toFuture(Some(form.copy(kind = Form.Kind.Active))))
        when(
          fixture.projectDao.getList(
            optId = *,
            optEventId = *,
            optGroupFromIds = *,
            optFormId = eqTo(Some(form.id)),
            optGroupAuditorId = *,
            optEmailTemplateId = *,
            optAnyRelatedGroupId = *
          )(*)
        ).thenReturn(toFuture(ListWithTotal[Project](0, Nil)))
        when(
          fixture.relationDao.getList(
            optId = *,
            optProjectId = *,
            optKind = *,
            optFormId = eqTo(Some(form.id)),
            optGroupFromId = *,
            optGroupToId = *,
            optEmailTemplateId = *
          )(*)
        ).thenReturn(toFuture(ListWithTotal[Relation](0, Nil)))
        when(fixture.formDaoMock.delete(form.id)).thenReturn(toFuture(1))

        val result = wait(fixture.service.delete(form.id).run)

        result mustBe right
      }
    }
  }
}
