package services

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import models.dao.{EventDao, ProjectDao, ProjectRelationDao, TemplateDao}
import models.event.Event
import models.notification.Notification
import models.project.Project
import models.template.Template
import play.api.libs.concurrent.Execution.Implicits._
import utils.Logger

import scala.concurrent.Future

/**
  * Email notifications service.
  */
@Singleton
class NotificationService @Inject()(
  eventDao: EventDao,
  projectDao: ProjectDao,
  relationDao: ProjectRelationDao,
  templateDao: TemplateDao,
  userService: UserService,
  mailService: MailService,
  templateEngineService: TemplateEngineService
) extends Logger {

  /**
    * Sends emails for event notifications between from and to.
    *
    * @param from time from
    * @param to   time to
    */
  def sendEventsNotifications(from: Timestamp, to: Timestamp): Future[Unit] = {
    for {
      events <- eventDao.getList(optNotificationFrom = Some(from), optNotificationTo = Some(to))
      _ <- Future.sequence(events.data.map(sendEventNotifications(_, from, to)))
    } yield ()
  }

  /**
    * Send all emails related to event.
    */
  private def sendEventNotifications(event: Event, from: Timestamp, to: Timestamp) = {

    /**
      * Sends emails to all users of group.
      *
      * @param groupId  group ID
      * @param template email template
      */
    def sendToGroupUsers(groupId: Long, template: Template) = {
      userService.listByGroupId(groupId).run.map { users =>
        users.toOption.get.data.foreach { user =>
          val context = templateEngineService.getContext(user, event)
          val body = templateEngineService.render(template.body, context)
          val subject = templateEngineService.render(template.subject, context)

          mailService.send(subject, user, body)
        }
      }
    }

    /**
      * Sends emails to all users of project's relations 'group-from'.
      *
      * @param project  project
      * @param template email template
      */
    def sendToRespondents(project: Project, template: Template) = {
      for {
        relations <- relationDao.getList(optProjectId = Some(project.id))
        _ <- Future.sequence {
          relations.data.map(relation => sendToGroupUsers(relation.groupFrom.id, template))
        }
      } yield ()
    }

    /**
      * Sends emails to auditor and respondent groups of project using appropriate project templates.
      *
      * @param project project
      */
    def sendToProject(project: Project) = {
      Future.sequence {
        for {
          notification <- event.notifications if notification.time.after(from) && notification.time.before(to)
          projectTemplate <- project.templates
          if notification.kind == projectTemplate.kind && notification.recipient == projectTemplate.recipient
        } yield {
          templateDao.findById(projectTemplate.template.id).flatMap { emailTemplate =>
            projectTemplate.recipient match {
              case Notification.Recipient.Auditor => sendToGroupUsers(project.groupAuditor.id, emailTemplate.get)
              case Notification.Recipient.Respondent => sendToRespondents(project, emailTemplate.get)
            }
          }
        }
      }
    }

    for {
      projects <- projectDao.getList(optEventId = Some(event.id))
      _ <- Future.sequence(projects.data.map(sendToProject))
    } yield ()
  }
}
