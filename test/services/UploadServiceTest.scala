package services

import java.sql.Timestamp

import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import models.dao.{EventDao, ProjectDao, ProjectRelationDao, UserDao}
import models.event.{Event, EventJob}
import models.form.Form
import models.project.Relation
import models.report.{AggregatedReport, Report}
import models.{ListWithTotal, NamedEntity}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import services.spreadsheet.SpreadsheetService
import testutils.fixture.{EventFixture, FormFixture, ProjectFixture, UserFixture}
import utils.errors.ApplicationError
import utils.listmeta.ListMeta

import scalaz.{\/, \/-, EitherT}

/**
  * Test for upload service
  */
class UploadServiceTest
  extends BaseServiceTest
  with EventFixture
  with ProjectFixture
  with FormFixture
  with UserFixture {

  private case class Fixture(
    eventDao: EventDao,
    projectDao: ProjectDao,
    reportService: ReportService,
    spreadsheetService: SpreadsheetService,
    formService: FormService,
    relationDao: ProjectRelationDao,
    userService: UserService,
    googleDriveService: GoogleDriveService,
    userDao: UserDao,
    service: UploadService
  )

  private def getFixture = {
    val eventDao = mock[EventDao]
    val projectDao = mock[ProjectDao]
    val reportService = mock[ReportService]
    val spreadsheetService = mock[SpreadsheetService]
    val formService = mock[FormService]
    val relationDao = mock[ProjectRelationDao]
    val userService = mock[UserService]
    val googleDriveService = mock[GoogleDriveService]
    val userDao = mock[UserDao]
    val service = new UploadService(eventDao,
                                    projectDao,
                                    reportService,
                                    spreadsheetService,
                                    formService,
                                    relationDao,
                                    userService,
                                    googleDriveService,
                                    userDao)
    Fixture(eventDao,
            projectDao,
            reportService,
            spreadsheetService,
            formService,
            relationDao,
            userService,
            googleDriveService,
            userDao,
            service)
  }

  private val jobFixture = EventJob.Upload(0, 1, new Timestamp(123), EventJob.Status.New)

  "getGroupedUploadModels" should {
    "return grouped upload models" in {
      val fixture = getFixture

      val from = new Timestamp(213)
      val to = new Timestamp(456)
      val event = Events(0).copy(id = 1)
      val project = Projects(0).copy(id = 2)
      val relation = Relation(
        id = 3,
        project = NamedEntity(4),
        groupFrom = NamedEntity(5),
        groupTo = Some(NamedEntity(6)),
        form = NamedEntity(7),
        kind = Relation.Kind.Classic,
        templates = Nil,
        hasInProgressEvents = false,
        canSelfVote = false
      )
      val freezedForm = Forms(0).copy(id = 8)
      val report = Report(None, Nil)
      val aggregatedReport = AggregatedReport(None, Nil)
      val user = Users(0)
      val batchUpdate = new BatchUpdateSpreadsheetRequest()

      when(
        fixture.eventDao.getList(
          optId = eqTo(Some(jobFixture.eventId)),
          optStatus = any[Option[Event.Status]],
          optProjectId = any[Option[Long]],
          optFormId = any[Option[Long]],
          optGroupFromIds = any[Option[Seq[Long]]]
        )(any[ListMeta])).thenReturn(toFuture(ListWithTotal(1, Seq(event))))
      when(
        fixture.projectDao.getList(
          optId = any[Option[Long]],
          optEventId = eqTo(Some(event.id)),
          optGroupFromIds = any[Option[Seq[Long]]],
          optFormId = any[Option[Long]],
          optGroupAuditorId = any[Option[Long]],
          optEmailTemplateId = any[Option[Long]],
          optAnyRelatedGroupId = any[Option[Long]]
        )(any[ListMeta])).thenReturn(toFuture(ListWithTotal(1, Seq(project))))
      when(
        fixture.relationDao.getList(
          optId = any[Option[Long]],
          optProjectId = eqTo(Some(project.id)),
          optKind = any[Option[Relation.Kind]],
          optFormId = any[Option[Long]],
          optGroupFromId = any[Option[Long]],
          optGroupToId = any[Option[Long]],
          optEmailTemplateId = any[Option[Long]]
        )(any[ListMeta])).thenReturn(toFuture(ListWithTotal(1, Seq(relation))))
      when(fixture.reportService.getReport(event.id, project.id)).thenReturn(toFuture(Seq(report)))
      when(fixture.formService.getOrCreateFreezedForm(event.id, relation.form.id))
        .thenReturn(EitherT.eitherT(toFuture(\/-(freezedForm): ApplicationError \/ Form)))
      when(fixture.reportService.getAggregatedReport(report)).thenReturn(aggregatedReport)
      when(fixture.spreadsheetService.getBatchUpdateRequest(Seq(report), Seq(aggregatedReport), Seq(freezedForm)))
        .thenReturn(batchUpdate)
      when(fixture.userService.getGroupIdToUsersMap(Seq(project.groupAuditor.id), false))
        .thenReturn(toFuture(Map(project.groupAuditor.id -> Seq(user))))

      val result = wait(fixture.service.getGroupedUploadModels(jobFixture.eventId))
      val expectedResult = Map(user -> Seq(UploadService.UploadModel(event, project, batchUpdate)))

      result mustBe expectedResult
    }
  }

  "upload" should {
    "upload reports to gdrive creating folder for user" in {
      val fixture = getFixture

      val user = Users(0).copy(id = 1)
      val event = Events(0).copy(id = 2)
      val project = Projects(0).copy(id = 3)
      val batchUpdate = new BatchUpdateSpreadsheetRequest
      val modelsWithUsers = Map(user -> Seq(UploadService.UploadModel(event, project, batchUpdate)))
      val userFolderId = "one"
      val eventFolderId = "onetwo"
      val spreadsheetId = "onetwofour"

      when(fixture.userDao.getGdriveFolder(user.id))
        .thenReturn(toFuture(None))
      when(fixture.googleDriveService.createFolder)
        .thenReturn((parentId: String, _: String) => if (parentId == "root") userFolderId else eventFolderId)
      when(fixture.userDao.setGdriveFolderId(user.id, userFolderId))
        .thenReturn(toFuture(userFolderId))
      when(fixture.googleDriveService.createSpreadsheet)
        .thenReturn((_: String, _: String) => spreadsheetId)

      wait(fixture.service.upload(modelsWithUsers))

      succeed
    }

    "upload reports to gdrive using existing folder for user" in {
      val fixture = getFixture

      val user = Users(0).copy(id = 1)
      val event = Events(0).copy(id = 2)
      val project = Projects(0).copy(id = 3)
      val batchUpdate = new BatchUpdateSpreadsheetRequest
      val modelsWithUsers = Map(user -> Seq(UploadService.UploadModel(event, project, batchUpdate)))
      val userFolderId = "one"
      val eventFolderId = "onetwo"
      val spreadsheetId = "onetwofour"

      when(fixture.userDao.getGdriveFolder(user.id))
        .thenReturn(toFuture(Some(userFolderId)))
      when(fixture.userDao.setGdriveFolderId(user.id, userFolderId))
        .thenReturn(toFuture(userFolderId))
      when(fixture.googleDriveService.createFolder)
        .thenReturn((_: String, _: String) => eventFolderId)
      when(fixture.googleDriveService.createSpreadsheet)
        .thenReturn((_: String, _: String) => spreadsheetId)

      wait(fixture.service.upload(modelsWithUsers))

      succeed
    }
  }
}
