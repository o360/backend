package services

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import models.dao.{EventDao, ProjectDao, ProjectRelationDao, TemplateDao}
import models.event.Event
import models.notification.Notification
import models.project.{Project, Relation, TemplateBinding}
import models.template.Template
import play.api.libs.concurrent.Execution.Implicits._

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
) {

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
    def sendToUsersInGroup(groupId: Long, template: Template) = {
      userService.listByGroupId(groupId).run.map { users =>
        users.toOption.map(_.data).getOrElse(Nil).foreach { user =>
          val context = templateEngineService.getContext(user, event)
          val body = templateEngineService.render(template.body, context)
          val subject = templateEngineService.render(template.subject, context)

          mailService.send(subject, user, body)
        }
      }
    }

    /**
      * Sends emails to auditor and respondent groups of project using appropriate relation templates.
      *
      * @param project project
      */
    def sendUsingRelationTemplates(project: Project, relations: Seq[Relation]) = Future.sequence {
      for {
        relation <- relations
        relationTemplate <- getActiveTemplates(relation.templates)
      } yield {
        sendUsingTemplateBinding(
          templateBinding = relationTemplate,
          auditorGroupId = project.groupAuditor.id,
          respondentGroupIds = Seq(relation.groupFrom.id)
        )
      }
    }

    /**
      * Sends emails to auditor and respondent groups of project using appropriate project templates.
      *
      * @param project project
      */
    def sendUsingProjectTemplates(project: Project, relations: Seq[Relation]) = Future.sequence {
      getActiveTemplates(project.templates).map { projectTemplate =>
        sendUsingTemplateBinding(
          templateBinding = projectTemplate,
          auditorGroupId = project.groupAuditor.id,
          respondentGroupIds = relations.map(_.groupFrom.id).distinct
        )
      }
    }

    /**
      * Sends emails using template binding.
      *
      * @param templateBinding    template binding
      * @param auditorGroupId     id of auditor group
      * @param respondentGroupIds ids of respondent groups
      */
    def sendUsingTemplateBinding(
      templateBinding: TemplateBinding,
      auditorGroupId: Long,
      respondentGroupIds: Seq[Long]
    ) = {
      templateDao.findById(templateBinding.template.id).flatMap { emailTemplate =>
        val template = emailTemplate.getOrElse(throw new NoSuchElementException("email template not found"))
        templateBinding.recipient match {
          case Notification.Recipient.Auditor =>
            sendToUsersInGroup(auditorGroupId, template)
          case Notification.Recipient.Respondent =>
            Future.sequence {
              respondentGroupIds.map(sendToUsersInGroup(_, template))
            }
        }
      }
    }

    /**
      * Returns templates for active notifications.
      *
      * @param templates templates
      */
    def getActiveTemplates(templates: Seq[TemplateBinding]) = {
      for {
        notification <- event.notifications if notification.time.after(from) && notification.time.before(to)
        template <- templates
        if notification.kind == template.kind && notification.recipient == template.recipient
      } yield template
    }

    for {
      projects <- projectDao.getList(optEventId = Some(event.id))
      _ <- Future.sequence {
        projects.data.map { project =>
          for {
            relations <- relationDao.getList(optProjectId = Some(project.id))
            _ <- sendUsingProjectTemplates(project, relations.data)
            _ <- sendUsingRelationTemplates(project, relations.data)
          } yield ()
        }
      }
    } yield ()
  }
}
