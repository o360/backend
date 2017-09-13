package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.NamedEntity
import models.assessment.Answer

/**
  * Answer model fixture.
  */
trait AnswerFixture extends FixtureHelper with ActiveProjectFixture with FormFixture with UserFixture {
  self: FixtureSupport =>

  val Answers = Seq(
    Answer(
      1,
      1,
      Some(3),
      NamedEntity(1),
      Answer.Status.Answered,
      isAnonymous = true,
      Set(
        Answer.Element(
          elementId = 1,
          text = Some("first answer"),
          valuesIds = None,
          comment = Some("comment")
        ),
        Answer.Element(
          elementId = 2,
          text = None,
          valuesIds = Some(Set(1)),
          comment = None
        )
      )
    ),
    Answer(
      2,
      1,
      None,
      NamedEntity(2),
      Answer.Status.New
    )
  )

  addFixtureOperation {
    sequenceOf(
      insertInto("form_answer")
        .columns("id", "active_project_id", "user_from_id", "user_to_id", "form_id", "is_anonymous", "status")
        .scalaValues(1, 1, 1, 3, 1, true, 1)
        .scalaValues(2, 2, 1, null, 2, false, 0)
        .build,
      insertInto("form_element_answer")
        .columns("id", "answer_id", "form_element_id", "text", "comment")
        .scalaValues(1, 1, 1, "first answer", "comment")
        .scalaValues(2, 1, 2, null, null)
        .build,
      insertInto("form_element_answer_value")
        .columns("id", "answer_element_id", "form_element_value_id")
        .scalaValues(1, 2, 1)
        .build
    )
  }
}
