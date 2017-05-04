package models.dao

import models.notification.Notification
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
      ) { (id: Option[Long], kind: Option[Notification.Kind], recipient: Option[Notification.Recipient]) =>
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
