package testutils.generator

import models.form.{Form, FormShort}
import org.scalacheck.{Arbitrary, Gen}

/**
  * Form generator for scalacheck.
  */
trait FormGenerator {

  implicit val elementValueArb = Arbitrary {
    for {
      value <- Arbitrary.arbitrary[String]
      caption <- Arbitrary.arbitrary[String]
    } yield Form.ElementValue(value, caption)
  }

  implicit val elementKindArb = Arbitrary[Form.ElementKind] {
    import Form.ElementKind._
    Gen.oneOf(TextField, TextArea, Checkbox, CheckboxGroup, Radio, Select)
  }

  implicit val elementArb = Arbitrary {
    for {
      kind <- Arbitrary.arbitrary[Form.ElementKind]
      caption <- Arbitrary.arbitrary[String]
      defaultValue <- Arbitrary.arbitrary[Option[String]]
      required <- Arbitrary.arbitrary[Boolean]
      values <- Arbitrary.arbitrary[Seq[Form.ElementValue]]
    } yield Form.Element(kind, caption, defaultValue, required, values)
  }

  implicit val formShortArb = Arbitrary {
    for {
      name <- Arbitrary.arbitrary[String]
    } yield FormShort(0, name)
  }

  implicit val formArb = Arbitrary {
    for {
      form <- Arbitrary.arbitrary[FormShort]
      elements <- Arbitrary.arbitrary[Seq[Form.Element]]
    } yield form.toModel(elements)
  }
}
