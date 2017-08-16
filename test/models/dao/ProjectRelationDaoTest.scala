package models.dao

import models.project.Relation
import org.scalacheck.Gen
import testutils.fixture.ProjectRelationFixture
import testutils.generator.ProjectRelationGenerator

/**
  * Test for project relation DAO.
  */
class ProjectRelationDaoTest extends BaseDaoTest with ProjectRelationFixture with ProjectRelationGenerator {

  private val dao = inject[ProjectRelationDao]

  "get" should {
    "return relations by specific criteria" in {
      forAll(Gen.option(Gen.choose(0L, 3L)), Gen.option(Gen.choose(0L, 3L))) {
        (id: Option[Long], projectId: Option[Long]) =>
          val projectRelations = wait(dao.getList(id, projectId))
          val expectedProjectRelations =
            ProjectRelations.filter(u => id.forall(_ == u.id) && projectId.forall(_ == u.project.id))
          projectRelations.total mustBe expectedProjectRelations.length
          projectRelations.data must contain theSameElementsAs expectedProjectRelations
      }
    }
  }

  "findById" should {
    "return relation by ID" in {
      forAll(Gen.choose(0L, ProjectRelations.length)) { (id: Long) =>
        val projectRelation = wait(dao.findById(id))
        val expectedProjectRelation = ProjectRelations.find(_.id == id)

        projectRelation mustBe expectedProjectRelation
      }
    }
  }

  "exists" should {
    "check relation for existing" in {
      wait(dao.exists(ProjectRelations(0))) mustBe true
      wait(dao.exists(ProjectRelations(1))) mustBe true

      forAll { (relation: Relation) =>
        whenever(
          !ProjectRelations
            .exists { x =>
              x.project.id == relation.project.id &&
              x.groupFrom.id == relation.groupFrom.id &&
              x.groupTo.map(_.id) == relation.groupTo.map(_.id) &&
              x.form.id == relation.form.id &&
              x.kind == relation.kind
            }) {

          wait(dao.exists(relation)) mustBe false
        }
      }
    }
  }

  "create" should {
    "create relation" in {
      forAll(Gen.oneOf(ProjectRelations)) { (relation: Relation) =>
        val created = wait(dao.create(relation))

        val projectRelationFromDb = wait(dao.findById(created.id))
        projectRelationFromDb mustBe defined
        created mustBe projectRelationFromDb.get
      }
    }
  }

  "delete" should {
    "delete relation" in {
      forAll(Gen.oneOf(ProjectRelations)) { (relation: Relation) =>
        val created = wait(dao.create(relation))

        val rowsDeleted = wait(dao.delete(created.id))

        val projectRelationFromDb = wait(dao.findById(created.id))
        rowsDeleted mustBe 1
        projectRelationFromDb mustBe empty
      }
    }
  }
  "update" should {
    "update relation" in {
      val newProjectRelationId = wait(dao.create(ProjectRelations(0))).id

      val expectedRelation = ProjectRelations(1).copy(id = newProjectRelationId)

      wait(dao.update(expectedRelation))

      val updatedFromDb = wait(dao.findById(newProjectRelationId))

      updatedFromDb mustBe defined
      updatedFromDb.get mustBe expectedRelation
    }
  }
}
