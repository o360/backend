package models.dao

import models.form.Form
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
      forAll { (form: Form) =>
        val created = wait(dao.create(form))

        val formFromDb = wait(dao.findById(created.id))
        val preparedForm = formFromDb.get.copy(id = 0, elements = formFromDb.get.elements.map { element =>
          element.copy(
            id = 0,
            values = element.values.map(_.copy(id = 0))
          )
        })
        preparedForm mustBe form
      }
    }
  }

  "delete" should {
    "delete form with elements" in {
      forAll { (form: Form) =>
        val created = wait(dao.create(form))

        val rowsDeleted = wait(dao.delete(created.id))

        val formFromDb = wait(dao.findById(created.id))
        rowsDeleted mustBe 1
        formFromDb mustBe empty
      }
    }
  }

  "update" should {
    "update form" in {
      val newFormId = wait(dao.create(Forms(0))).id

      forAll { (form: Form) =>
        val formWithId = form.copy(id = newFormId)

        wait(dao.update(formWithId))

        val updatedFromDb = wait(dao.findById(newFormId))
        val preparedForm = updatedFromDb.get.copy(id = 0, elements = updatedFromDb.get.elements.map { element =>
          element.copy(
            id = 0,
            values = element.values.map(_.copy(id = 0))
          )
        })

        preparedForm mustBe form
      }
    }
  }
}
