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
