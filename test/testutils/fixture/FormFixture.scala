package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.form.Form

/**
  * Form model fixture.
  */
trait FormFixture extends FixtureHelper {
  self: FixtureSupport =>

  val Forms = Seq(
    Form(
      1,
      "first",
      Seq(
        Form.Element(
          Form.ElementKind.TextField,
          "cap1",
          Some("def1"),
          required = true,
          Nil
        ),
        Form.Element(
          Form.ElementKind.Radio,
          "cap2",
          None,
          required = false,
          Seq(
            Form.ElementValue(
              "val1",
              "cap1"
            ),
            Form.ElementValue(
              "val2",
              "cap2"
            )
          )
        )
      )
    ),
    Form(
      2,
      "second",
      Nil
    )
  )

  val FormsShort = Forms.map(_.toShort)

  addFixtureOperation {
    sequenceOf(
      insertInto("form")
        .columns("id", "name")
        .scalaValues(1, "first")
        .scalaValues(2, "second")
        .build,
      insertInto("form_element")
        .columns("id", "form_id", "kind", "caption", "default_value", "required", "ord")
        .scalaValues(1, 1, 0, "cap1", "def1", true, 1)
        .scalaValues(2, 1, 4, "cap2", null, false, 2)
        .build,
      insertInto("form_element_value")
        .columns("id", "element_id", "value", "caption", "ord")
        .scalaValues(1, 2, "val1", "cap1", 1)
        .scalaValues(2, 2, "val2", "cap2", 2)
        .build
    )
  }
}
