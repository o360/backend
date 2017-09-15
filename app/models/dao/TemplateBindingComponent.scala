package models.dao

import models.NamedEntity
import models.notification._
import models.project.TemplateBinding
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

/**
  * Component for template binding tables.
  */
trait TemplateBindingComponent extends NotificationComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  /**
    * Template binding DM model.
    */
  case class DbTemplateBinding(
    ownerId: Long,
    templateId: Long,
    kind: NotificationKind,
    recipient: NotificationRecipient
  ) {
    def toModel(templateName: String) = TemplateBinding(
      NamedEntity(templateId, templateName),
      kind,
      recipient
    )
  }

  object DbTemplateBinding {
    def fromModel(binding: TemplateBinding, ownerId: Long) = DbTemplateBinding(
      ownerId,
      binding.template.id,
      binding.kind,
      binding.recipient
    )
  }

  class ProjectTemplateBindingTable(tag: Tag) extends Table[DbTemplateBinding](tag, "project_email_template") {

    def projectId = column[Long]("project_id")
    def templateId = column[Long]("template_id")
    def kind = column[NotificationKind]("kind")
    def recipient = column[NotificationRecipient]("recipient_kind")

    def * = (projectId, templateId, kind, recipient) <> ((DbTemplateBinding.apply _).tupled, DbTemplateBinding.unapply)
  }

  val ProjectTemplates = TableQuery[ProjectTemplateBindingTable]

  class RelationTemplateBindingTable(tag: Tag) extends Table[DbTemplateBinding](tag, "relation_email_template") {

    def relationId = column[Long]("relation_id")
    def templateId = column[Long]("template_id")
    def kind = column[NotificationKind]("kind")
    def recipient = column[NotificationRecipient]("recipient_kind")

    def * =
      (relationId, templateId, kind, recipient) <> ((DbTemplateBinding.apply _).tupled, DbTemplateBinding.unapply)
  }

  val RelationTemplates = TableQuery[RelationTemplateBindingTable]
}
