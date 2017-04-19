package models.dao

import models.form.{Form, FormShort}
import org.scalacheck.Gen
import testutils.fixture.FormFixture
import testutils.generator.FormGenerator

/**
  * Test for form DAO.
  */
class FormDaoTest extends BaseDaoTest with FormFixture with FormGenerator {

  private val dao = inject[FormDao]

  "findById" should {
    "return form with elements by ID" in {
      forAll(Gen.choose(0L, Forms.length)) { (id: Long) =>
        val form = wait(dao.findById(id))
        val expectedForm = Forms.find(_.id == id)

        form mustBe expectedForm
      }
    }
  }

  "getList" should {
    "return forms list" in {
      val forms = wait(dao.getList())

      forms.total mustBe FormsShort.length
      forms.data must contain theSameElementsAs FormsShort
    }
  }

  "create" should {
    "create form" in {
      forAll { (form: FormShort) =>
        val created = wait(dao.create(form))

        val formFromDb = wait(dao.findById(created.id)).map(_.toShort)
        formFromDb mustBe defined
        created mustBe formFromDb.get
      }
    }
  }

  "createElements" should {
    "create elements in db" in {
      forAll { (form: Form) =>
        val created = wait(dao.create(form.toShort))

        val createdElements = wait(dao.createElements(created.id, form.elements))

        val formFromDb = wait(dao.findById(created.id))
        formFromDb mustBe defined
        formFromDb.get mustBe form.copy(id = created.id)
      }
    }
  }

  "delete" should {
    "delete form with elements" in {
      forAll { (form: Form) =>
        val created = wait(dao.create(form.toShort))
        wait(dao.createElements(created.id, form.elements))

        val rowsDeleted = wait(dao.delete(created.id))

        val formFromDb = wait(dao.findById(created.id))
        rowsDeleted mustBe 1
        formFromDb mustBe empty
      }
    }
  }

  "delete elements" should {
    "delete form elements" in {
      forAll { (form: Form) =>
        val created = wait(dao.create(form.toShort))
        wait(dao.createElements(created.id, form.elements))

        val rowsDeleted = wait(dao.deleteElements(created.id))

        val formFromDb = wait(dao.findById(created.id))
        rowsDeleted mustBe form.elements.length
        formFromDb mustBe defined
        formFromDb.get.elements mustBe empty
      }
    }
  }

  "update" should {
    "update form" in {
      val newFormId = wait(dao.create(FormsShort(0))).id

      forAll { (form: FormShort) =>
        val formWithId = form.copy(id = newFormId)

        wait(dao.update(formWithId))

        val updatedFromDb = wait(dao.findById(newFormId))

        updatedFromDb mustBe defined
        updatedFromDb.get.toShort mustBe formWithId
      }
    }
  }
}
