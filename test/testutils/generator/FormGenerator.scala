package testutils.generator

import models.form.{Form, FormShort}
import org.scalacheck.{Arbitrary, Gen}

/**
  * Form generator for scalacheck.
  */
trait FormGenerator {

  implicit val elementValueArb = Arbitrary {
      Arbitrary.arbitrary[String].map(Form.ElementValue(0, _))
  }

  implicit val elementKindArb = Arbitrary[Form.ElementKind] {
    import Form.ElementKind._
    Gen.oneOf(TextField, TextArea, Checkbox, CheckboxGroup, Radio, Select)
  }

  implicit val elementArb = Arbitrary {
    for {
      kind <- Arbitrary.arbitrary[Form.ElementKind]
      caption <- Arbitrary.arbitrary[String]
      required <- Arbitrary.arbitrary[Boolean]
      values <- Arbitrary.arbitrary[Seq[Form.ElementValue]]
    } yield Form.Element(0, kind, caption, required, values)
  }

  implicit val formKindArb = Arbitrary[Form.Kind] {
    import Form.Kind._
    Gen.oneOf(Active, Freezed)
  }

  implicit val formShortArb = Arbitrary {
    for {
      name <- Arbitrary.arbitrary[String]
      kind <- Arbitrary.arbitrary[Form.Kind]
      showInAggregation <- Arbitrary.arbitrary[Boolean]
      machineName <- Arbitrary.arbitrary[String]
    } yield FormShort(0, name, kind, showInAggregation, machineName)
  }

  implicit val formArb = Arbitrary {
    for {
      form <- Arbitrary.arbitrary[FormShort]
      elements <- Arbitrary.arbitrary[Seq[Form.Element]]
    } yield form.withElements(elements)
  }
}
