package services

import models.ListWithTotal
import models.dao.{EventDao, ProjectDao, ProjectRelationDao, TemplateDao}
import models.notification.Notification
import models.project.{Project, Relation}
import models.template.Template
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import testutils.fixture.{ProjectFixture, TemplateFixture, UserFixture}
import testutils.generator.TemplateGenerator
import utils.errors.{ConflictError, NotFoundError}
import utils.listmeta.ListMeta

/**
  * Test for template service.
  */
class TemplateServiceTest extends BaseServiceTest with TemplateGenerator with TemplateFixture with ProjectFixture {

  private val admin = UserFixture.admin

  private case class TestFixture(
    templateDaoMock: TemplateDao,
    eventDaoMock: EventDao,
    projectDao: ProjectDao,
    relationDao: ProjectRelationDao,
    service: TemplateService
  )

  private def getFixture = {
    val daoMock = mock[TemplateDao]
    val eventDaoMock = mock[EventDao]
    val projectDao = mock[ProjectDao]
    val relationDao = mock[ProjectRelationDao]
    val service = new TemplateService(daoMock, eventDaoMock, projectDao, relationDao, ec)
    TestFixture(daoMock, eventDaoMock, projectDao, relationDao, service)
  }

  "getById" should {

    "return not found if template not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.templateDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.getById(id)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.templateDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.templateDaoMock)
      }
    }

    "return template from db" in {
      forAll { (template: Template, id: Long) =>
        val fixture = getFixture
        when(fixture.templateDaoMock.findById(id)).thenReturn(toFuture(Some(template)))
        val result = wait(fixture.service.getById(id)(admin).run)

        result mustBe 'right
        result.toOption.get mustBe template

        verify(fixture.templateDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.templateDaoMock)
      }
    }
  }

  "list" should {
    "return list of templates from db" in {
      forAll {
        (
          kind: Option[Notification.Kind],
          recipient: Option[Notification.Recipient],
          templates: Seq[Template],
          total: Int
        ) =>
          val fixture = getFixture
          when(
            fixture.templateDaoMock.getList(
              optId = any[Option[Long]],
              optKind = eqTo(kind),
              optRecipient = eqTo(recipient)
            )(eqTo(ListMeta.default)))
            .thenReturn(toFuture(ListWithTotal(total, templates)))
          val result = wait(fixture.service.getList(kind, recipient)(admin, ListMeta.default).run)

          result mustBe 'right
          result.toOption.get mustBe ListWithTotal(total, templates)
      }
    }
  }

  "create" should {
    "create template in db" in {
      val template = Templates(0)

      val fixture = getFixture
      when(fixture.templateDaoMock.create(template.copy(id = 0))).thenReturn(toFuture(template))
      val result = wait(fixture.service.create(template.copy(id = 0))(admin).run)

      result mustBe 'right
      result.toOption.get mustBe template
    }
  }

  "update" should {
    "return not found if template not found" in {
      forAll { (template: Template) =>
        val fixture = getFixture
        when(fixture.templateDaoMock.findById(template.id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.update(template)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.templateDaoMock, times(1)).findById(template.id)
        verifyNoMoreInteractions(fixture.templateDaoMock)
      }
    }

    "update template in db" in {
      val template = Templates(0)
      val fixture = getFixture
      when(fixture.templateDaoMock.findById(template.id)).thenReturn(toFuture(Some(template)))
      when(fixture.templateDaoMock.update(template)).thenReturn(toFuture(template))
      val result = wait(fixture.service.update(template)(admin).run)

      result mustBe 'right
      result.toOption.get mustBe template
    }
  }

  "delete" should {
    "return not found if template not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.templateDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.delete(id)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.templateDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.templateDaoMock)
      }
    }

    "return conflict error if can't delete" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.templateDaoMock.findById(id)).thenReturn(toFuture(Some(Templates(0))))
        when(
          fixture.projectDao.getList(
            optId = any[Option[Long]],
            optEventId = any[Option[Long]],
            optGroupFromIds = any[Option[Seq[Long]]],
            optFormId = any[Option[Long]],
            optGroupAuditorId = any[Option[Long]],
            optEmailTemplateId = eqTo(Some(id)),
            optAnyRelatedGroupId = any[Option[Long]]
          )(any[ListMeta])).thenReturn(toFuture(ListWithTotal(1, Projects.take(1))))
        when(
          fixture.relationDao.getList(
            optId = any[Option[Long]],
            optProjectId = any[Option[Long]],
            optKind = any[Option[Relation.Kind]],
            optFormId = any[Option[Long]],
            optGroupFromId = any[Option[Long]],
            optGroupToId = any[Option[Long]],
            optEmailTemplateId = eqTo(Some(id))
          )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Relation](0, Nil)))

        val result = wait(fixture.service.delete(id)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[ConflictError]
      }
    }

    "delete template from db" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.templateDaoMock.findById(id)).thenReturn(toFuture(Some(Templates(0))))
        when(
          fixture.projectDao.getList(
            optId = any[Option[Long]],
            optEventId = any[Option[Long]],
            optGroupFromIds = any[Option[Seq[Long]]],
            optFormId = any[Option[Long]],
            optGroupAuditorId = any[Option[Long]],
            optEmailTemplateId = eqTo(Some(id)),
            optAnyRelatedGroupId = any[Option[Long]]
          )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Project](0, Nil)))
        when(
          fixture.relationDao.getList(
            optId = any[Option[Long]],
            optProjectId = any[Option[Long]],
            optKind = any[Option[Relation.Kind]],
            optFormId = any[Option[Long]],
            optGroupFromId = any[Option[Long]],
            optGroupToId = any[Option[Long]],
            optEmailTemplateId = eqTo(Some(id))
          )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Relation](0, Nil)))

        when(fixture.templateDaoMock.delete(id)).thenReturn(toFuture(1))

        val result = wait(fixture.service.delete(id)(admin).run)

        result mustBe 'right
      }
    }
  }
}
