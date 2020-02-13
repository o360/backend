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

import models.notification._
import models.template.Template
import org.scalacheck.Gen
import testutils.fixture.TemplateFixture
import testutils.generator.TemplateGenerator

/**
  * Test for template DAO.
  */
class TemplateDaoTest extends BaseDaoTest with TemplateFixture with TemplateGenerator {

  private val dao = inject[TemplateDao]

  "get" should {
    "return templates by specific criteria" in {
      forAll(
        Gen.option(Gen.choose(0L, 3L)),
        Gen.option(notificationKindArb.arbitrary),
        Gen.option(notificationRecipientArb.arbitrary)
      ) { (id: Option[Long], kind: Option[NotificationKind], recipient: Option[NotificationRecipient]) =>
        val templates = wait(dao.getList(id, kind, recipient))
        val expectedTemplates =
          Templates.filter(t => id.forall(_ == t.id) && kind.forall(_ == t.kind) && recipient.forall(_ == t.recipient))
        templates.total mustBe expectedTemplates.length
        templates.data must contain theSameElementsAs expectedTemplates
      }
    }
  }

  "findById" should {
    "return template by ID" in {
      forAll(Gen.choose(0L, Templates.length)) { (id: Long) =>
        val template = wait(dao.findById(id))
        val expectedTemplate = Templates.find(_.id == id)

        template mustBe expectedTemplate
      }
    }
  }

  "create" should {
    "create template" in {
      forAll { (template: Template) =>
        val created = wait(dao.create(template))

        val templateFromDb = wait(dao.findById(created.id))
        templateFromDb mustBe defined
        created mustBe templateFromDb.get
      }
    }
  }

  "delete" should {
    "delete template" in {
      forAll { (template: Template) =>
        val created = wait(dao.create(template))

        val rowsDeleted = wait(dao.delete(created.id))

        val templateFromDb = wait(dao.findById(created.id))
        rowsDeleted mustBe 1
        templateFromDb mustBe empty
      }
    }
  }
  "update" should {
    "update template" in {
      val newTemplateId = wait(dao.create(Templates(0))).id

      forAll { (template: Template) =>
        val templateWithId = template.copy(id = newTemplateId)

        wait(dao.update(templateWithId))

        val updatedFromDb = wait(dao.findById(newTemplateId))

        updatedFromDb mustBe defined
        updatedFromDb.get mustBe templateWithId
      }
    }
  }
}
