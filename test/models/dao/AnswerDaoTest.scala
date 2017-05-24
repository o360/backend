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

      val result = wait(dao.getAnswer(10, 20, 30, None, 40))

      result mustBe empty
    }

    "return answer" in {
      val result1 = wait(dao.getAnswer(1, 1, 1, Some(3), 1))
      val result2 = wait(dao.getAnswer(1, 1, 1, None, 2))

      result1 mustBe Some(Answers(0))
      result2 mustBe Some(Answers(1))
    }
  }

// TODO: For some reason scalacheck generates 0 for elementId and test fails
//  "saveAnswer" should {
//    "saveAnswer in DB" in {
//      forAll(answerFormArb.arbitrary, Gen.oneOf(1L, 2L, 3L, 4L), Gen.listOf(Gen.oneOf(1L, 2L, 3L, 4L))) { (
//      answer: Answer.Form,
//      elementId: Long,
//      elementValueIds: Seq[Long]) =>
//        val preparedAnswer = answer.copy(
//          answers = answer.answers.map(_.copy(elementId = elementId, valuesIds = Some(elementValueIds))),
//          form = NamedEntity(1))
//
//        val result = wait(dao.saveAnswer(1, 1, 1, None, preparedAnswer))
//        val answerFromDb = wait(dao.getAnswer(1, 1, 1, None, 1))
//
//        answerFromDb mustBe defined
//        result mustBe answerFromDb.get
//      }
//    }
//  }

}
