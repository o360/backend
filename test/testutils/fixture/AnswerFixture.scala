package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.NamedEntity
import models.assessment.Answer

/**
  * Event model fixture.
  */
trait AnswerFixture extends FixtureHelper with EventFixture with ProjectFixture with FormFixture with UserFixture {
  self: FixtureSupport =>

  val Answers = Seq(
    Answer.Form(
      form = NamedEntity(1, "first"),
      answers = Set(
        Answer.Element(
          elementId = 1,
          text = Some("first answer"),
          valuesIds = None
        ),
        Answer.Element(
          elementId = 2,
          text = None,
          valuesIds = Some(Set(1))
        )
      ),
      isAnonymous = true
    ),
    Answer.Form(
      form = NamedEntity(2, "second"),
      answers = Set(),
      isAnonymous = false
    )
  )

  addFixtureOperation {
    sequenceOf(
      insertInto("form_answer")
        .columns("id", "event_id", "project_id", "user_from_id", "user_to_id", "form_id", "is_anonymous")
        .scalaValues(1, 1, 1, 1, 3, 1, true)
        .scalaValues(2, 1, 1, 1, null, 2, false)
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
