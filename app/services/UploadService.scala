package services

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import models.dao.{EventDao, ProjectDao, ProjectRelationDao, UserDao}
import models.event.Event
import models.form.Form
import models.project.{Project, Relation}
import models.user.User
import services.UploadService.UploadModel

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import utils.implicits.FutureLifting._

/**
  * Upload service.
  */
@Singleton
class UploadService @Inject()(
  eventDao: EventDao,
  projectDao: ProjectDao,
  reportService: ReportService,
  spreadsheetService: SpreadsheetService,
  formService: FormService,
  relationDao: ProjectRelationDao,
  userService: UserService,
  googleDriveService: GoogleDriveService,
  userDao: UserDao
) {

  /**
    * Returns upload models grouped by user.
    *
    * @param from events end time from
    * @param to   events end time to
    */
  def getGroupedUploadModels(from: Timestamp, to: Timestamp): Future[Map[User, Seq[UploadModel]]] = {


    /**
      * Returns forms for given relations and event.
      */
    def getForms(relations: Seq[Relation], eventId: Long): Future[Seq[Form]] = {
      Future.sequence {
        relations.map(_.form.id).distinct.map { formTemplateId =>
          formService
            .getOrCreateFreezedForm(eventId, formTemplateId)
            .run
            .map(_.getOrElse(throw new NoSuchElementException("missed form")))
        }
      }
    }

    /**
      * Returns events with its projects.
      */
    def withProjects(events: Seq[Event]): Future[Seq[(Event, Project)]] = {
      Future.sequence {
        events.map { event =>
          projectDao
            .getList(optEventId = Some(event.id))
            .map { projects => projects.data.map((event, _)) }
        }
      }
    }.map(_.flatten)

    /**
      * Returns upload models for given events with projects.
      */
    def getUploadModels(eventsWithProjects: Seq[(Event, Project)]): Future[Seq[UploadModel]] = {
      Future.sequence {
        eventsWithProjects.map { case (event, project) =>
          for {
            relations <- relationDao.getList(optProjectId = Some(project.id))
            reports <- reportService.getReport(event.id, project.id)
            forms <- getForms(relations.data, event.id)
          } yield {
            val sortedReports = reports.sortWith{ (left, right) =>
              val leftName = left.assessedUser.flatMap(_.name)
              val rightName = right.assessedUser.flatMap(_.name)
              (leftName, rightName) match {
                case (None, None) => false
                case (Some(l), Some(r)) => l > r
                case (Some(_), None) => true
                case (None, Some(_)) => false
              }
            }

            val aggregatedReports = sortedReports.map(reportService.getAggregatedReport)
            val batchUpdate = spreadsheetService.getBatchUpdateRequest(sortedReports, aggregatedReports, forms)
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
      val getGroupToUsersMap: Future[Map[Long, Seq[User]]] = {
        val allGroups = uploadModels.map(_.project.groupAuditor.id).distinct
        userService.getGroupIdToUsersMap(allGroups, includeDeleted = false)
      }

      getGroupToUsersMap.map { groupToUsersMap =>
        uploadModels.flatMap { uploadModel =>
          val users = groupToUsersMap(uploadModel.project.groupAuditor.id)
          users.map((_, uploadModel))
        }
          .groupBy(_._1)
          .mapValues(_.map(_._2))
      }
    }


    for {
      events <- eventDao.getList(optEndFrom = Some(from), optEndTimeTo = Some(to))
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

    Future.sequence {
      modelsWithUsers.map { case (user, models) =>
        for {
          userFolderId <- getOrCreateUserFolder(user)
        } yield {
          models.groupBy(_.event).foreach { case (event, eventModels) =>
            val eventFolderId = googleDriveService.createFolder(userFolderId, event.caption)

            eventModels.foreach { case UploadModel(_, project, batchUpdate) =>
              val spreadSheetId = googleDriveService.createSpreadsheet(eventFolderId, project.name)
              googleDriveService.applyBatchSpreadsheetUpdate(spreadSheetId, batchUpdate)
            }
          }
        }
      }
    }.map(_ => ())
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
  case class UploadModel(event: Event, project: Project, spreadSheetBatch: BatchUpdateSpreadsheetRequest)
}
