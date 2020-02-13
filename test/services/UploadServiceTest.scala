/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import java.time.LocalDateTime

import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import models.ListWithTotal
import models.dao._
import models.event.EventJob
import models.report.{AggregatedReport, Report}
import org.mockito.Mockito._
import services.spreadsheet.SpreadsheetService
import testutils.fixture._

/**
  * Test for upload service
  */
class UploadServiceTest extends BaseServiceTest with EventFixture with FormFixture with UserFixture {

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

  private val jobFixture = EventJob.Upload(0, 1, LocalDateTime.MAX, EventJob.Status.New)

  "getGroupedUploadModels" should {
    "return grouped upload models" in {
      val fixture = getFixture

      val event = Events(0).copy(id = 1)
      val activeProject = ActiveProjectFixture.values(0).copy(id = 2)
      val report = Report(None, Nil)
      val aggregatedReport = AggregatedReport(None, Nil)
      val user = Users(0)
      val batchUpdate = new BatchUpdateSpreadsheetRequest()

      when(
        fixture.eventDao.getList(
          optId = eqTo(Some(jobFixture.eventId)),
          optStatus = *,
          optProjectId = *,
          optFormId = *,
          optGroupFromIds = *,
          optUserId = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal(1, Seq(event))))
      when(
        fixture.activeProjectDao.getList(
          optId = *,
          optUserId = *,
          optEventId = eqTo(Some(event.id))
        )(*)
      ).thenReturn(toFuture(ListWithTotal(1, Seq(activeProject))))

      when(fixture.reportService.getReport(activeProject.id)).thenReturn(toFuture(Seq(report)))
      when(fixture.reportService.getAggregatedReport(report)).thenReturn(aggregatedReport)
      when(fixture.spreadsheetService.getBatchUpdateRequest(Seq(report), Seq(aggregatedReport))).thenReturn(batchUpdate)
      when(
        fixture.userDao.getList(
          optIds = *,
          optRole = *,
          optStatus = *,
          optGroupIds = *,
          optName = *,
          optEmail = *,
          optProjectIdAuditor = eqTo(Some(activeProject.id)),
          includeDeleted = *
        )(*)
      ).thenReturn(toFuture(ListWithTotal(Seq(user))))

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
      val activeProject = ActiveProjectFixture.values(0).copy(id = 3)
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
      val activeProject = ActiveProjectFixture.values(0).copy(id = 3)
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
