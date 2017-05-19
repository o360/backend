package models.dao

import testutils.fixture.AnswerFixture

/**
  * Test for answer DAO.
  */
class AnswerDaoTest extends BaseDaoTest with AnswerFixture {

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

}
