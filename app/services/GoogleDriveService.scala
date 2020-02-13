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

import java.io.FileInputStream
import javax.inject.Singleton

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.{HttpRequest, HttpRequestInitializer}
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.model.{File, Permission}
import com.google.api.services.drive.{Drive, DriveScopes}
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.{Sheets, SheetsScopes}
import utils.Logger

import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

/**
  * Google drive service.
  */
@Singleton
class GoogleDriveService extends Logger {

  private lazy val credential = GoogleCredential
    .fromStream(new FileInputStream("conf/drive_service_key.json"))
    .createScoped(Seq(DriveScopes.DRIVE, SheetsScopes.SPREADSHEETS).asJava)

  private lazy val requestInitializer = new HttpRequestInitializer {
    override def initialize(request: HttpRequest): Unit = {
      credential.initialize(request)
      request.setConnectTimeout(60000) // 1 minute
      request.setReadTimeout(10 * 60000) // 10 minutes
    }
  }
  private lazy val transport = new NetHttpTransport()
  private lazy val jsonFactory = JacksonFactory.getDefaultInstance
  private lazy val driveService = new Drive.Builder(transport, jsonFactory, requestInitializer).build
  private lazy val sheetsService = new Sheets.Builder(transport, jsonFactory, requestInitializer).build

  /**
    * Creates object in gdrive.
    *
    * @param contentType content type
    * @param parent      parentId
    * @param name        name
    * @return created object ID
    */
  private def createObject(contentType: String)(parent: String, name: String) = {
    try {
      val file = new File()
        .setMimeType(contentType)
        .setName(name)
        .setParents(Seq(parent).asJava)
      val result = driveService.files.create(file).execute()
      result.getId
    } catch {
      case NonFatal(e) =>
        log.error("gdrive object creation failed", e)
        throw e
    }
  }

  /**
    * Creates folder in gdrive.
    */
  val createFolder = createObject("application/vnd.google-apps.folder")(_, _)

  /**
    * Creates spreadsheet in gdrive.
    */
  val createSpreadsheet = createObject("application/vnd.google-apps.spreadsheet")(_, _)

  /**
    * Sets permissions on file.
    *
    * @param fileId file id
    * @param email  access granted to email
    */
  def setPermission(fileId: String, email: String): Unit = {
    try {
      val permission = new Permission()
        .setRole("writer")
        .setType("user")
        .setEmailAddress(email)
      driveService.permissions.create(fileId, permission).execute()
    } catch {
      case NonFatal(e) => log.error("setting google drive permissions", e)
    }
  }

  /**
    * Applies batch spreadsheet update to spreadsheet.
    *
    * @param fileId      spreadsheet ID
    * @param batchUpdate batch update
    */
  def applyBatchSpreadsheetUpdate(fileId: String, batchUpdate: BatchUpdateSpreadsheetRequest): Unit = {
    try {
      sheetsService.spreadsheets.batchUpdate(fileId, batchUpdate).execute()
    } catch {
      case NonFatal(e) =>
        log.error("applying spreadsheet batch update failed", e)
        throw e
    }
  }

  /**
    * Checks if folder exists.
    *
    * @param folderId ID of folder
    * @return true if folder exists
    */
  def isFolderExists(folderId: String): Boolean = {
    try {
      driveService.files().get(folderId).execute()
      true
    } catch {
      case NonFatal(e) =>
        log.error("getting gdrive file failed, returning false", e)
        false
    }
  }
}
