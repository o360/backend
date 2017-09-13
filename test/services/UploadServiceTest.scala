package services

import java.sql.Timestamp

import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import models.ListWithTotal
import models.dao._
import models.event.{Event, EventJob}
import models.report.{AggregatedReport, Report}
import models.user.User
import org.davidbild.tristate.Tristate
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import services.spreadsheet.SpreadsheetService
import testutils.fixture._
import utils.listmeta.ListMeta

/**
  * Test for upload service
  */
class UploadServiceTest
  extends BaseServiceTest
  with EventFixture
  with ActiveProjectFixture
  with FormFixture
  with UserFixture {

  private case class Fixture(
    eventDao: EventDao,
    activeProjectDao: ActiveProjectDao,
    reportService: ReportService,
    spreadsheetService: SpreadsheetService,
    userDao: UserDao,
    googleDriveService: GoogleDriveService,
    service: UploadService
  )

  private def getFixture = {
    val eventDao = mock[EventDao]
    val activeProjectDao = mock[ActiveProjectDao]
    val reportService = mock[ReportService]
    val spreadsheetService = mock[SpreadsheetService]
    val userDao = mock[UserDao]
    val googleDriveService = mock[GoogleDriveService]
    val service = new UploadService(
      eventDao,
      activeProjectDao,
      reportService,
      spreadsheetService,
      userDao,
      googleDriveService,
      ec
    )
    Fixture(eventDao, activeProjectDao, reportService, spreadsheetService, userDao, googleDriveService, service)
  }

  private val jobFixture = EventJob.Upload(0, 1, new Timestamp(123), EventJob.Status.New)

  "getGroupedUploadModels" should {
    "return grouped upload models" in {
      val fixture = getFixture

      val event = Events(0).copy(id = 1)
      val activeProject = ActiveProjects(0).copy(id = 2)
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
          optGroupFromIds = any[Option[Seq[Long]]],
          optUserId = any[Option[Long]]
        )(any[ListMeta])).thenReturn(toFuture(ListWithTotal(1, Seq(event))))
      when(
        fixture.activeProjectDao.getList(
          optId = any[Option[Long]],
          optUserId = any[Option[Long]],
          optEventId = eqTo(Some(event.id))
        )(any[ListMeta])).thenReturn(toFuture(ListWithTotal(1, Seq(activeProject))))

      when(fixture.reportService.getReport(activeProject.id)).thenReturn(toFuture(Seq(report)))
      when(fixture.reportService.getAggregatedReport(report)).thenReturn(aggregatedReport)
      when(fixture.spreadsheetService.getBatchUpdateRequest(Seq(report), Seq(aggregatedReport))).thenReturn(batchUpdate)
      when(fixture.userDao.getList(
        optIds = any[Option[Seq[Long]]],
        optRole = any[Option[User.Role]],
        optStatus = any[Option[User.Status]],
        optGroupIds = any[Tristate[Seq[Long]]],
        optName = any[Option[String]],
        optEmail = any[Option[String]],
        optProjectIdAuditor = eqTo(Some(activeProject.id)),
        includeDeleted = any[Boolean],
      )(any[ListMeta]))
        .thenReturn(toFuture(ListWithTotal(Seq(user))))

      val result = wait(fixture.service.getGroupedUploadModels(jobFixture.eventId))
      val expectedResult = Map(user -> Seq(UploadService.UploadModel(event, activeProject, batchUpdate)))

      result mustBe expectedResult
    }
  }

  "upload" should {
    "upload reports to gdrive creating folder for user" in {
      val fixture = getFixture

      val user = Users(0).copy(id = 1)
      val event = Events(0).copy(id = 2)
      val activeProject = ActiveProjects(0).copy(id = 3)
      val batchUpdate = new BatchUpdateSpreadsheetRequest
      val modelsWithUsers = Map(user -> Seq(UploadService.UploadModel(event, activeProject, batchUpdate)))
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
      val activeProject = ActiveProjects(0).copy(id = 3)
      val batchUpdate = new BatchUpdateSpreadsheetRequest
      val modelsWithUsers = Map(user -> Seq(UploadService.UploadModel(event, activeProject, batchUpdate)))
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
