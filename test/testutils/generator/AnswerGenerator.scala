package testutils.generator

import models.assessment.Answer
import org.scalacheck.Arbitrary

/**
  * Answer generator for scalacheck.
  */
trait AnswerGenerator {

  implicit val answerElementArb = Arbitrary {
    for {
      elementId <- Arbitrary.arbitrary[Long]
      text <- Arbitrary.arbitrary[Option[String]]
      values <- Arbitrary.arbitrary[Option[Seq[Long]]]
    } yield Answer.Element(elementId, text, values)
  }

  implicit val answerFormArb = Arbitrary {
    for {
      id <- Arbitrary.arbitrary[Long]
      answers <- Arbitrary.arbitrary[Set[Answer.Element]]
    } yield Answer.Form(id, answers)
  }
}
