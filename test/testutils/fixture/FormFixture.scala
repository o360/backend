package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.form.Form
import models.form.element._

/**
  * Form model fixture.
  */
trait FormFixture extends FixtureHelper { self: FixtureSupport =>

  val Forms = Seq(
    Form(
      1,
      "first",
      Seq(
        Form.Element(
          1,
          TextField,
          "cap1",
          required = true,
          Nil
        ),
        Form.Element(
          2,
          Radio,
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
      ),
      Form.Kind.Active,
      showInAggregation = true,
      machineName = "machine 1"
    ),
    Form(
      2,
      "second",
      Nil,
      Form.Kind.Active,
      showInAggregation = false,
      machineName = "machine 2"
    ),
    Form(
      3,
      "first",
      Seq(
        Form.Element(
          3,
          TextField,
          "cap1",
          required = true,
          Nil
        ),
        Form.Element(
          4,
          Radio,
          "cap2",
          required = false,
          Seq(
            Form.ElementValue(
              3,
              "cap1"
            ),
            Form.ElementValue(
              4,
              "cap2"
            )
          )
        )
      ),
      Form.Kind.Freezed,
      showInAggregation = true,
      machineName = "machine 1"
    )
  )

  val FormsShort = Forms.map(_.toShort)

  addFixtureOperation {
    sequenceOf(
      insertInto("form")
        .columns("id", "name", "kind", "show_in_aggregation", "machine_name")
        .scalaValues(1, "first", 0, true, "machine 1")
        .scalaValues(2, "second", 0, false, "machine 2")
        .scalaValues(3, "first", 1, true, "machine 1")
        .build,
      insertInto("form_element")
        .columns("id", "form_id", "kind", "caption", "required", "ord")
        .scalaValues(1, 1, 0, "cap1", true, 1)
        .scalaValues(2, 1, 4, "cap2", false, 2)
        .scalaValues(3, 3, 0, "cap1", true, 1)
        .scalaValues(4, 3, 4, "cap2", false, 2)
        .build,
      insertInto("form_element_value")
        .columns("id", "element_id", "caption", "ord")
        .scalaValues(1, 2, "cap1", 1)
        .scalaValues(2, 2, "cap2", 2)
        .scalaValues(3, 4, "cap1", 1)
        .scalaValues(4, 4, "cap2", 2)
        .build
    )
  }
}
