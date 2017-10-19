package testutils.generator

import models.NamedEntity
import models.assessment.Answer
import org.scalacheck.{Arbitrary, Gen}

/**
  * Answer generator for scalacheck.
  */
trait AnswerGenerator {

  implicit val answerStatusArb: Arbitrary[Answer.Status] = Arbitrary {
    import Answer.Status._
    Gen.oneOf(New, Answered, Skipped)
  }

  implicit val answerElementArb: Arbitrary[Answer.Element] = Arbitrary {
    for {
      elementId <- Arbitrary.arbitrary[Long]
      text <- Arbitrary.arbitrary[Option[String]]
      values <- Arbitrary.arbitrary[Option[Seq[Long]]]
      comment <- Arbitrary.arbitrary[Option[String]]
    } yield Answer.Element(elementId, text, values.map(_.toSet), comment)
  }

  implicit val answerFormArb: Arbitrary[Answer] = Arbitrary {
    for {
      activeProjectId <- Arbitrary.arbitrary[Long]
      userFromId <- Arbitrary.arbitrary[Long]
      userToId <- Arbitrary.arbitrary[Option[Long]]
      formId <- Arbitrary.arbitrary[Long]
      formId <- Arbitrary.arbitrary[Long]
      isAnonymous <- Arbitrary.arbitrary[Boolean]
      answers <- Arbitrary.arbitrary[Set[Answer.Element]]
      canSkip <- Arbitrary.arbitrary[Boolean]
      status <- answerStatusArb.arbitrary
    } yield Answer(activeProjectId, userFromId, userToId, NamedEntity(formId), canSkip, status, isAnonymous, answers)
  }
}
