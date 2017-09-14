package services

import java.io.FileInputStream
import javax.inject.Singleton

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.model.{File, Permission}
import com.google.api.services.drive.{Drive, DriveScopes}
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.{Sheets, SheetsScopes}
import utils.Logger

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
  * Google drive service.
  */
@Singleton
class GoogleDriveService extends Logger {

  private lazy val credential = GoogleCredential
    .fromStream(new FileInputStream("conf/drive_service_key.json"))
    .createScoped(Seq(DriveScopes.DRIVE, SheetsScopes.SPREADSHEETS).asJava)
  private lazy val transport = new NetHttpTransport()
  private lazy val jsonFactory = JacksonFactory.getDefaultInstance
  private lazy val driveService = new Drive.Builder(transport, jsonFactory, credential).build
  private lazy val sheetsService = new Sheets.Builder(transport, jsonFactory, credential).build

  /**
    * Creates object in gdrive.
    *
    * @param contentType content type
    * @param parent      parentId
    * @param name        name
    * @return created object ID
    */
  private def createObject(contentType: String)(parent: String, name: String) = {
    val file = new File()
      .setMimeType(contentType)
      .setName(name)
      .setParents(Seq(parent).asJava)
    val result = driveService.files.create(file).execute()
    result.getId
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
    sheetsService.spreadsheets.batchUpdate(fileId, batchUpdate).execute()
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
      case e: Throwable =>
        e.printStackTrace()
        false
    }
  }
}
