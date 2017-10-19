package models.dao

import models.EntityKind
import models.competence.CompetenceGroup
import org.scalacheck.Gen
import testutils.fixture.CompetenceGroupFixture
import testutils.generator.CompetenceGroupGenerator

/**
  * Test for competence group DAO.
  */
class CompetenceGroupDaoTest extends BaseDaoTest with CompetenceGroupFixture with CompetenceGroupGenerator {

  private val dao = inject[CompetenceGroupDao]

  "get" should {
    "return groups by specific criteria" in {
      forAll(
        Gen.option(Gen.someOf(CompetenceGroups.map(_.id))),
        Gen.option(Gen.oneOf[EntityKind](EntityKind.Template, EntityKind.Freezed))
      ) { (ids, kind) =>
        val groups = wait(dao.getList(kind, ids))
        val expectedGroups = CompetenceGroups.filter(cg => ids.forall(_.contains(cg.id)) && kind.forall(_ == cg.kind))
        groups.total mustBe expectedGroups.length
        groups.data must contain theSameElementsAs expectedGroups
      }
    }
  }

  "findById" should {
    "return group by ID" in {
      forAll(Gen.oneOf(CompetenceGroups.map(_.id) :+ -10L)) { id =>
        val group = wait(dao.getById(id))
        val expectedGroup = CompetenceGroups.find(_.id == id)

        group mustBe expectedGroup
      }
    }
  }

  "create" should {
    "create group" in {
      forAll { (group: CompetenceGroup) =>
        val created = wait(dao.create(group))

        val groupFromDb = wait(dao.getById(created.id))
        groupFromDb mustBe defined
        created mustBe groupFromDb.get
      }
    }
  }

  "delete" should {
    "delete group" in {
      forAll { (group: CompetenceGroup) =>
        val created = wait(dao.create(group))

        wait(dao.delete(created.id))

        val groupFromDb = wait(dao.getById(created.id))
        groupFromDb mustBe empty
      }
    }
  }
  "update" should {
    "update group" in {
      val newGroupId = wait(dao.create(CompetenceGroups(0))).id

      forAll { (group: CompetenceGroup) =>
        val groupWithId = group.copy(id = newGroupId)

        wait(dao.update(groupWithId))

        val updatedFromDb = wait(dao.getById(newGroupId))

        updatedFromDb mustBe defined
        updatedFromDb.get mustBe groupWithId
      }
    }
  }
}
