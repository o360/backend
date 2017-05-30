package services

import java.sql.Timestamp

import models.{ListWithTotal, NamedEntity}
import models.dao.{EventDao, ProjectRelationDao}
import models.event.Event
import models.form.Form
import models.project.Relation
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import testutils.fixture.{FormFixture, ProjectRelationFixture, UserFixture}
import testutils.generator.ProjectRelationGenerator
import utils.errors.{ApplicationError, BadRequestError, ConflictError, NotFoundError}
import utils.listmeta.ListMeta

import scalaz._
import Scalaz.ToEitherOps

/**
  * Test for project relation service.
  */
class ProjectRelationServiceTest
  extends BaseServiceTest
    with ProjectRelationGenerator
    with ProjectRelationFixture
    with FormFixture {

  private val admin = UserFixture.admin

  private case class TestFixture(
    relationDaoMock: ProjectRelationDao,
    eventDaoMock: EventDao,
    formService: FormService,
    service: ProjectRelationService)

  private def getFixture = {
    val daoMock = mock[ProjectRelationDao]
    val eventDaoMock = mock[EventDao]
    val formService = mock[FormService]
    val service = new ProjectRelationService(daoMock, eventDaoMock, formService)
    TestFixture(daoMock, eventDaoMock, formService, service)
  }

  "getById" should {

    "return not found if relation not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.relationDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.getById(id)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.relationDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.relationDaoMock)
      }
    }

    "return relation from db" in {
      forAll { (relation: Relation, id: Long) =>
        val fixture = getFixture
        when(fixture.relationDaoMock.findById(id)).thenReturn(toFuture(Some(relation)))
        val result = wait(fixture.service.getById(id)(admin).run)

        result mustBe 'right
        result.toOption.get mustBe relation

        verify(fixture.relationDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.relationDaoMock)
      }
    }
  }

  "list" should {
    "return list of relations from db" in {
      forAll { (
      projectId: Option[Long],
      relations: Seq[Relation],
      total: Int
      ) =>
        val fixture = getFixture
        when(fixture.relationDaoMock.getList(
          optId = any[Option[Long]],
          optProjectId = eqTo(projectId),
          optKind = any[Option[Relation.Kind]],
          optFormId = any[Option[Long]],
          optGroupFromId = any[Option[Long]],
          optGroupToId = any[Option[Long]],
          optEmailTemplateId = any[Option[Long]]
        )(eqTo(ListMeta.default))
        ).thenReturn(toFuture(ListWithTotal(total, relations)))
        val result = wait(fixture.service.getList(projectId)(admin, ListMeta.default).run)

        result mustBe 'right
        result.toOption.get mustBe ListWithTotal(total, relations)
      }
    }
  }

  "create" should {
    "return bad request if can't validate relations" in {
      val fixture = getFixture
      val relation = Relation(1, NamedEntity(1), NamedEntity(1), None, NamedEntity(2), Relation.Kind.Classic, Nil)

      val result = wait(fixture.service.create(relation)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[BadRequestError]
    }


    "return bad request if relation already exists" in {
      val fixture = getFixture
      val relation = ProjectRelations(0)
      val form = Forms(0).copy(kind = Form.Kind.Active)

      when(fixture.relationDaoMock.exists(relation)).thenReturn(toFuture(true))
      when(fixture.eventDaoMock.getList(
        optId = any[Option[Long]],
        optStatus = eqTo(Some(Event.Status.InProgress)),
        optProjectId = eqTo(Some(relation.project.id)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))
      when(fixture.formService.getById(relation.form.id))
        .thenReturn(EitherT.eitherT(toFuture(form.right[ApplicationError])))
      val result = wait(fixture.service.create(relation)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[BadRequestError]
    }

    "return conflict if exists events in progress" in {
      val fixture = getFixture
      val relation = ProjectRelations(0)

      when(fixture.relationDaoMock.exists(relation)).thenReturn(toFuture(false))
      when(fixture.eventDaoMock.getList(
        optId = any[Option[Long]],
        optStatus = eqTo(Some(Event.Status.InProgress)),
        optProjectId = eqTo(Some(relation.project.id)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Event](1, Nil)))
      val result = wait(fixture.service.create(relation)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "return conflict if used form is freezed" in {
      val relation = ProjectRelations(0)
      val form = Forms(0).copy(kind = Form.Kind.Freezed)

      val fixture = getFixture

      when(fixture.relationDaoMock.exists(relation.copy(id = 0))).thenReturn(toFuture(false))
      when(fixture.relationDaoMock.create(relation.copy(id = 0))).thenReturn(toFuture(relation))
      when(fixture.eventDaoMock.getList(
        optId = any[Option[Long]],
        optStatus = eqTo(Some(Event.Status.InProgress)),
        optProjectId = eqTo(Some(relation.project.id)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))
      when(fixture.formService.getById(relation.form.id))
        .thenReturn(EitherT.eitherT(toFuture(form.right[ApplicationError])))
      val result = wait(fixture.service.create(relation.copy(id = 0))(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "create relation in db" in {
      val relation = ProjectRelations(0)
      val form = Forms(0).copy(kind = Form.Kind.Active)

      val fixture = getFixture

      when(fixture.relationDaoMock.exists(relation.copy(id = 0))).thenReturn(toFuture(false))
      when(fixture.relationDaoMock.create(relation.copy(id = 0))).thenReturn(toFuture(relation))
      when(fixture.eventDaoMock.getList(
        optId = any[Option[Long]],
        optStatus = eqTo(Some(Event.Status.InProgress)),
        optProjectId = eqTo(Some(relation.project.id)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))
      when(fixture.formService.getById(relation.form.id))
        .thenReturn(EitherT.eitherT(toFuture(form.right[ApplicationError])))
      val result = wait(fixture.service.create(relation.copy(id = 0))(admin).run)

      result mustBe 'right
      result.toOption.get mustBe relation
    }
  }

  "update" should {
    "return not found if can't find relation" in {
      val fixture = getFixture
      val relation = ProjectRelations(0)

      when(fixture.relationDaoMock.findById(relation.id)).thenReturn(toFuture(None))

      val result = wait(fixture.service.update(relation)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[NotFoundError]
    }

    "return bad request if projectId is changed" in {
      val fixture = getFixture
      val relation = ProjectRelations(0)

      when(fixture.relationDaoMock.findById(relation.id))
        .thenReturn(toFuture(Some(relation.copy(project = NamedEntity(999)))))
      val result = wait(fixture.service.update(relation)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[BadRequestError]
    }

    "return bad request if can't validate relations" in {
      val fixture = getFixture
      val relation = Relation(1, NamedEntity(1), NamedEntity(1), None, NamedEntity(2), Relation.Kind.Classic, Nil)

      when(fixture.relationDaoMock.findById(relation.id)).thenReturn(toFuture(Some(relation)))
      when(fixture.eventDaoMock.getList(
        optId = any[Option[Long]],
        optStatus = eqTo(Some(Event.Status.InProgress)),
        optProjectId = eqTo(Some(relation.project.id)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))
      val result = wait(fixture.service.update(relation)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[BadRequestError]
    }


    "return bad request if relation already exists" in {
      val fixture = getFixture
      val relation = ProjectRelations(0)
      val form = Forms(0).copy(kind = Form.Kind.Active)

      when(fixture.relationDaoMock.findById(relation.id))
        .thenReturn(toFuture(Some(relation.copy(groupFrom = NamedEntity(999)))))
      when(fixture.relationDaoMock.exists(relation)).thenReturn(toFuture(true))
      when(fixture.eventDaoMock.getList(
        optId = any[Option[Long]],
        optStatus = eqTo(Some(Event.Status.InProgress)),
        optProjectId = eqTo(Some(relation.project.id)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))
      when(fixture.formService.getById(relation.form.id))
        .thenReturn(EitherT.eitherT(toFuture(form.right[ApplicationError])))
      val result = wait(fixture.service.update(relation)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[BadRequestError]
    }

    "return conflict if exists events in progress" in {
      val fixture = getFixture
      val relation = ProjectRelations(0)

      when(fixture.relationDaoMock.findById(relation.id)).thenReturn(toFuture(Some(relation)))
      when(fixture.relationDaoMock.exists(relation)).thenReturn(toFuture(false))
      when(fixture.eventDaoMock.getList(
        optId = any[Option[Long]],
        optStatus = eqTo(Some(Event.Status.InProgress)),
        optProjectId = eqTo(Some(relation.project.id)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Event](1, Nil)))
      val result = wait(fixture.service.update(relation)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "return conflict if used form is freezed" in {
      val relation = ProjectRelations(0)
      val form = Forms(0).copy(kind = Form.Kind.Freezed)

      val fixture = getFixture

      when(fixture.relationDaoMock.findById(relation.id)).thenReturn(toFuture(Some(relation)))
      when(fixture.relationDaoMock.exists(relation)).thenReturn(toFuture(false))
      when(fixture.relationDaoMock.update(relation)).thenReturn(toFuture(relation))
      when(fixture.eventDaoMock.getList(
        optId = any[Option[Long]],
        optStatus = eqTo(Some(Event.Status.InProgress)),
        optProjectId = eqTo(Some(relation.project.id)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))
      when(fixture.formService.getById(relation.form.id))
        .thenReturn(EitherT.eitherT(toFuture(form.right[ApplicationError])))
      val result = wait(fixture.service.update(relation)(admin).run)

      result mustBe 'left
      result.swap.toOption.get mustBe a[ConflictError]
    }

    "update relation in db" in {
      val relation = ProjectRelations(0)
      val form = Forms(0).copy(kind = Form.Kind.Active)

      val fixture = getFixture

      when(fixture.relationDaoMock.findById(relation.id)).thenReturn(toFuture(Some(relation)))
      when(fixture.relationDaoMock.exists(relation)).thenReturn(toFuture(false))
      when(fixture.relationDaoMock.update(relation)).thenReturn(toFuture(relation))
      when(fixture.eventDaoMock.getList(
        optId = any[Option[Long]],
        optStatus = eqTo(Some(Event.Status.InProgress)),
        optProjectId = eqTo(Some(relation.project.id)),
        optNotificationFrom = any[Option[Timestamp]],
        optNotificationTo = any[Option[Timestamp]],
        optFormId = any[Option[Long]],
        optGroupFromIds = any[Option[Seq[Long]]],
        optEndFrom = any[Option[Timestamp]],
        optEndTimeTo = any[Option[Timestamp]]
      )(any[ListMeta])).thenReturn(toFuture(ListWithTotal[Event](0, Nil)))
      when(fixture.formService.getById(relation.form.id))
        .thenReturn(EitherT.eitherT(toFuture(form.right[ApplicationError])))
      val result = wait(fixture.service.update(relation)(admin).run)

      result mustBe 'right
      result.toOption.get mustBe relation
    }
  }

  "delete" should {
    "return not found if relation not found" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.relationDaoMock.findById(id)).thenReturn(toFuture(None))
        val result = wait(fixture.service.delete(id)(admin).run)

        result mustBe 'left
        result.swap.toOption.get mustBe a[NotFoundError]

        verify(fixture.relationDaoMock, times(1)).findById(id)
        verifyNoMoreInteractions(fixture.relationDaoMock)
      }
    }

    "delete relation from db" in {
      forAll { (id: Long) =>
        val fixture = getFixture
        when(fixture.relationDaoMock.findById(id)).thenReturn(toFuture(Some(ProjectRelations(0))))
        when(fixture.relationDaoMock.delete(id)).thenReturn(toFuture(1))

        val result = wait(fixture.service.delete(id)(admin).run)

        result mustBe 'right
      }
    }
  }
}
