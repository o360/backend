package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.form.Form
import models.form.element._

/**
  * Form model fixture.
  */
trait FormFixture extends FixtureHelper { self: FixtureSupport =>

  val Forms = FormFixture.values

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
        .columns("id", "form_id", "kind", "caption", "required", "ord", "machine_name", "hint")
        .scalaValues(1, 1, 0, "cap1", true, 1, "el mach name", null)
        .scalaValues(2, 1, 4, "cap2", false, 2, "el 2 mach name", "hint1")
        .scalaValues(3, 3, 0, "cap1", true, 1, "el 3 mach name", null)
        .scalaValues(4, 3, 4, "cap2", false, 2, "el 4 mach name", "hint2")
        .build,
      insertInto("form_element_value")
        .columns("id", "element_id", "caption", "ord", "competence_weight")
        .scalaValues(1, 2, "cap1", 1, 15)
        .scalaValues(2, 2, "cap2", 2, 27.5)
        .scalaValues(3, 4, "cap1", 1, null)
        .scalaValues(4, 4, "cap2", 2, null)
        .build
    )
  }
}

object FormFixture {
  val values = Seq(
    Form(
      1,
      "first",
      Seq(
        Form.Element(
          1,
          TextField,
          "cap1",
          required = true,
          Nil,
          Nil,
          "el mach name",
          None
        ),
        Form.Element(
          2,
          Radio,
          "cap2",
          required = false,
          Seq(
            Form.ElementValue(
              1,
              "cap1",
              Some(15)
            ),
            Form.ElementValue(
              2,
              "cap2",
              Some(27.5)
            )
          ),
          Nil,
          "el 2 mach name",
          Some("hint1")
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
          Nil,
          Nil,
          "el 3 mach name",
          None
        ),
        Form.Element(
          4,
          Radio,
          "cap2",
          required = false,
          Seq(
            Form.ElementValue(
              3,
              "cap1",
              None
            ),
            Form.ElementValue(
              4,
              "cap2",
              None
            )
          ),
          Nil,
          "el 4 mach name",
          Some("hint2")
        )
      ),
      Form.Kind.Freezed,
      showInAggregation = true,
      machineName = "machine 1"
    )
  )
}
