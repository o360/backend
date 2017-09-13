package models.dao

import models.NamedEntity
import models.assessment.Answer
import org.scalacheck.Gen
import testutils.fixture.AnswerFixture
import testutils.generator.AnswerGenerator

/**
  * Test for answer DAO.
  */
class AnswerDaoTest extends BaseDaoTest with AnswerFixture with AnswerGenerator {

  private val dao = inject[AnswerDao]

  "getAnswer" should {
    "return None if no answer" in {

      val result = wait(dao.getAnswer(20, 30, None, 40))

      result mustBe empty
    }

    "return answer" in {
      val result1 = wait(dao.getAnswer(1, 1, Some(3), 1))
      val result2 = wait(dao.getAnswer(2, 1, None, 2))

      result1 mustBe Some(Answers(0))
      result2 mustBe Some(Answers(1))
    }
  }

  "getAnswers" should {
    "return answers for event" in {
      val result = wait(dao.getList(optEventId = Some(1))).toSet

      val expectedResult = Set(Answers(0))

      result mustBe expectedResult
    }
  }

  "saveAnswer" should {
    "saveAnswer in DB" in {
      forAll(Gen.choose[Long](1, 4), Gen.nonEmptyListOf(Gen.choose[Long](1, 4)), answerFormArb.arbitrary) {
        (elementId: Long, elementValueIds: Seq[Long], answer: Answer) =>
          val preparedAnswer =
            answer.copy(
              elements = answer.elements.map(_.copy(elementId = elementId, valuesIds = Some(elementValueIds.toSet))),
              form = NamedEntity(1),
              userFromId = 1,
              userToId = None,
              activeProjectId = 1
            )

          val result = wait(dao.saveAnswer(preparedAnswer))
          val answerFromDb = wait(dao.getAnswer(1, 1, None, 1))

          answerFromDb mustBe defined
          result mustBe answerFromDb.get
      }
    }
  }
}
