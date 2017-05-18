package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.assessment.Answer

/**
  * Event model fixture.
  */
trait AnswerFixture extends FixtureHelper with EventFixture with ProjectFixture with FormFixture with UserFixture {
  self: FixtureSupport =>

  val Answers = Seq(
    Answer.Form(
      formId = 1,
      answers = Set(
        Answer.Element(
          elementId = 1,
          text = Some("first answer"),
          valuesIds = None
        ),
        Answer.Element(
          elementId = 2,
          text = None,
          valuesIds = Some(Seq(1))
        )
      )
    ),
    Answer.Form(
      formId = 2,
      answers = Set()
    )
  )

  addFixtureOperation {
    sequenceOf(
      insertInto("form_answer")
        .columns("id", "event_id", "project_id", "user_from_id", "user_to_id", "form_id")
        .scalaValues(1, 1, 1, 1, 3, 1)
        .scalaValues(2, 1, 1, 1, null, 2)
        .build,
      insertInto("form_element_answer")
        .columns("id", "answer_id", "form_element_id", "text")
        .scalaValues(1, 1, 1, "first answer")
        .scalaValues(2, 1, 2, null)
        .build,
      insertInto("form_element_answer_value")
        .columns("id", "answer_element_id", "form_element_value_id")
        .scalaValues(1, 2, 1)
        .build
    )
  }
}
