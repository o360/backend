/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
      isAnonymous <- Arbitrary.arbitrary[Boolean]
      answers <- Arbitrary.arbitrary[Set[Answer.Element]]
      canSkip <- Arbitrary.arbitrary[Boolean]
      status <- answerStatusArb.arbitrary
    } yield Answer(activeProjectId, userFromId, userToId, NamedEntity(formId), canSkip, status, isAnonymous, answers)
  }
}
