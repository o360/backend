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
class UploadService @Inject()(
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
      Future.sequence {
        eventsWithProjects.map {
          case (event, project) =>
            for {
              reports <- reportService.getReport(project.id)
            } yield {
              val sortedReports = reports.sortWith { (left, right) =>
                val leftName = left.assessedUser.flatMap(_.name)
                val rightName = right.assessedUser.flatMap(_.name)
                (leftName, rightName) match {
                  case (None, None) => false
                  case (Some(l), Some(r)) => l < r
                  case (Some(_), None) => true
                  case (None, Some(_)) => false
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
            allProjectsIds.map(id => userDao.getList(optProjectIdAuditor = Some(id)).map(list => (id, list.data))))
          .map(_.toMap)
      }

      getProjectToUsersMap.map { projectToUsersMap =>
        uploadModels
          .flatMap { uploadModel =>
            val users = projectToUsersMap(uploadModel.project.id)
            users.map((_, uploadModel))
          }
          .groupBy(_._1)
          .mapValues(_.map(_._2))
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
        val userFolderId = googleDriveService.createFolder("root", s"Report for ${user.name.getOrElse("")}")
        googleDriveService.setPermission(userFolderId, user.email.getOrElse(""))
        userDao.setGdriveFolderId(user.id, userFolderId)
    }

    Future
      .sequence {
        modelsWithUsers.map {
          case (user, models) =>
            for {
              userFolderId <- getOrCreateUserFolder(user)
            } yield {
              models.groupBy(_.event).foreach {
                case (event, eventModels) =>
                  val eventFolderId = googleDriveService.createFolder(userFolderId, event.caption(user.timezone))

                  eventModels.foreach {
                    case UploadModel(_, project, batchUpdate) =>
                      val spreadSheetId = googleDriveService.createSpreadsheet(eventFolderId, project.name)
                      googleDriveService.applyBatchSpreadsheetUpdate(spreadSheetId, batchUpdate)
                  }
              }
            }
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
