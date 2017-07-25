package testutils.generator

import models.NamedEntity
import models.assessment.Answer
import org.scalacheck.Arbitrary

/**
  * Answer generator for scalacheck.
  */
trait AnswerGenerator {

  implicit val answerElementArb: Arbitrary[Answer.Element] = Arbitrary {
    for {
      elementId <- Arbitrary.arbitrary[Long]
      text <- Arbitrary.arbitrary[Option[String]]
      values <- Arbitrary.arbitrary[Option[Seq[Long]]]
    } yield Answer.Element(elementId, text, values.map(_.toSet))
  }

  implicit val answerFormArb: Arbitrary[Answer.Form] = Arbitrary {
    for {
      id <- Arbitrary.arbitrary[Long]
      answers <- Arbitrary.arbitrary[Set[Answer.Element]]
      isAnonymous <- Arbitrary.arbitrary[Boolean]
    } yield Answer.Form(NamedEntity(id), answers, isAnonymous)
  }
}
