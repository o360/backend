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
          1,
          Form.ElementKind.TextField,
          "cap1",
          required = true,
          Nil
        ),
        Form.Element(
          2,
          Form.ElementKind.Radio,
          "cap2",
          required = false,
          Seq(
            Form.ElementValue(
              1,
              "cap1"
            ),
            Form.ElementValue(
              2,
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
        .columns("id", "form_id", "kind", "caption", "required", "ord")
        .scalaValues(1, 1, 0, "cap1", true, 1)
        .scalaValues(2, 1, 4, "cap2", false, 2)
        .build,
      insertInto("form_element_value")
        .columns("id", "element_id", "caption", "ord")
        .scalaValues(1, 2, "cap1", 1)
        .scalaValues(2, 2, "cap2", 2)
        .build
    )
  }
}
