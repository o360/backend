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

import javax.inject.{Inject, Singleton}

import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import models.dao._
import models.event.{Event, EventJob}
import models.project.ActiveProject
import models.user.User
import services.UploadService.UploadModel
import services.spreadsheet.SpreadsheetService
import utils.implicits.FutureLifting._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Upload service.
  */
@Singleton
class UploadService @Inject() (
  eventDao: EventDao,
  activeProjectDao: ActiveProjectDao,
  reportService: ReportService,
  spreadsheetService: SpreadsheetService,
  userDao: UserDao,
  googleDriveService: GoogleDriveService,
  implicit val ec: ExecutionContext
) {

  /**
    * Executes upload job.
    */
  def execute(job: EventJob.Upload): Future[Unit] = {
    for {
      groupedUploadModels <- getGroupedUploadModels(job.eventId)
      _ <- upload(groupedUploadModels)
    } yield ()
  }

  /**
    * Returns upload models grouped by user.
    */
  def getGroupedUploadModels(eventId: Long): Future[Map[User, Seq[UploadModel]]] = {

    /**
      * Returns events with its projects.
      */
    def withProjects(events: Seq[Event]): Future[Seq[(Event, ActiveProject)]] = {
      Future.sequence {
        events.map { event =>
          activeProjectDao
            .getList(optEventId = Some(event.id))
            .map { projects =>
              projects.data.map((event, _))
            }
        }
      }
    }.map(_.flatten)

    /**
      * Returns upload models for given events with projects.
      */
    def getUploadModels(eventsWithProjects: Seq[(Event, ActiveProject)]): Future[Seq[UploadModel]] = {

      def compareOptionsWith[T](x: Option[T], y: Option[T])(f: (T, T) => Boolean): Boolean = {
        (x, y) match {
          case (Some(_), None)    => true
          case (None, Some(_))    => false
          case (None, None)       => false
          case (Some(x), Some(y)) => f(x, y)
        }
      }

      def compareOptionStrings(x: Option[String], y: Option[String]): Boolean =
        compareOptionsWith(x, y)(_.toLowerCase > _.toLowerCase)

      Future.sequence {
        eventsWithProjects.map {
          case (event, project) =>
            for {
              reports <- reportService.getReport(project.id)
            } yield {
              val sortedReports = reports.sortWith { (left, right) =>
                compareOptionsWith(left.assessedUser, right.assessedUser) { (leftUser, rightUser) =>
                  compareOptionStrings(leftUser.lastName, rightUser.lastName) ||
                  compareOptionStrings(leftUser.firstName, rightUser.firstName)
                }
              }

              val aggregatedReports = sortedReports.map(reportService.getAggregatedReport)
              val batchUpdate = spreadsheetService.getBatchUpdateRequest(sortedReports, aggregatedReports)
              UploadModel(event, project, batchUpdate)
            }
        }
      }
    }

    /**
      * Groups upload models by user.
      */
    def groupByAuditor(uploadModels: Seq[UploadModel]): Future[Map[User, Seq[UploadModel]]] = {

      /**
        * Map from groupId to sequence of group users.
        */
      val getProjectToUsersMap: Future[Map[Long, Seq[User]]] = {
        val allProjectsIds = uploadModels.map(_.project.id).distinct
        Future
          .sequence(
            allProjectsIds.map(id => userDao.getList(optProjectIdAuditor = Some(id)).map(list => (id, list.data)))
          )
          .map(_.toMap)
      }

      getProjectToUsersMap.map { projectToUsersMap =>
        uploadModels
          .flatMap { uploadModel =>
            val users = projectToUsersMap(uploadModel.project.id)
            users.map((_, uploadModel))
          }
          .groupBy(_._1)
          .view
          .mapValues(_.map(_._2))
          .toMap
      }
    }

    for {
      events <- eventDao.getList(optId = Some(eventId))
      eventsWithProjects <- withProjects(events.data)
      uploadModels <- getUploadModels(eventsWithProjects)
      groupedUploadModels <- groupByAuditor(uploadModels)
    } yield groupedUploadModels
  }

  /**
    * Uploads reports to gdrive service account and shares access with auditors.
    */
  def upload(modelsWithUsers: Map[User, Seq[UploadModel]]): Future[Unit] = {

    def getOrCreateUserFolder(user: User) = userDao.getGdriveFolder(user.id).flatMap {
      case Some(folderId) =>
        val email = user.email.getOrElse(throw new NoSuchElementException(s"missed auditor $user email"))
        googleDriveService.setPermission(folderId, email)
        folderId.toFuture
      case None =>
        val userFolderId = googleDriveService.createFolder("root", s"Report for ${user.firstName.getOrElse("")}")
        googleDriveService.setPermission(userFolderId, user.email.getOrElse(""))
        userDao.setGdriveFolderId(user.id, userFolderId)
    }

    Future
      .sequence {
        modelsWithUsers.map {
          case (user, models) =>
            if (models.nonEmpty) {
              for {
                userFolderId <- getOrCreateUserFolder(user)
              } yield {
                models.groupBy(_.event).foreach {
                  case (event, eventModels) =>
                    if (eventModels.nonEmpty) {
                      val eventFolderId = googleDriveService.createFolder(userFolderId, event.caption(user.timezone))

                      eventModels.foreach {
                        case UploadModel(_, project, batchUpdate) =>
                          val spreadSheetId = googleDriveService.createSpreadsheet(eventFolderId, project.name)
                          googleDriveService.applyBatchSpreadsheetUpdate(spreadSheetId, batchUpdate)
                      }
                    } else ().toFuture
                }
              }
            } else ().toFuture
        }
      }
      .map(_ => ())
  }
}

object UploadService {

  /**
    * Upload model.
    *
    * @param event            event
    * @param project          project
    * @param spreadSheetBatch batch spreadsheet update with report content
    */
  case class UploadModel(event: Event, project: ActiveProject, spreadSheetBatch: BatchUpdateSpreadsheetRequest)
}
