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

import models.project.ActiveProject
import org.scalacheck.{Arbitrary, Gen}
import testutils.fixture.{EventFixture, ProjectFixture}

/**
  * Active project generator for scalacheck.
  */
trait ActiveProjectGenerator {

  implicit val activeProjectArb = Arbitrary {
    for {
      id <- Arbitrary.arbitrary[Long]
      eventId <- Gen.oneOf(EventFixture.values.map(_.id))
      name <- Arbitrary.arbitrary[String]
      description <- Arbitrary.arbitrary[Option[String]]
      formsOnSamePage <- Arbitrary.arbitrary[Boolean]
      canRevote <- Arbitrary.arbitrary[Boolean]
      isAnonymous <- Arbitrary.arbitrary[Boolean]
      machineName <- Arbitrary.arbitrary[String]
      parentProjectId <- Gen.option(Gen.oneOf(ProjectFixture.values.map(_.id)))
    } yield ActiveProject(
      id,
      eventId,
      name,
      description,
      formsOnSamePage,
      canRevote,
      isAnonymous,
      machineName,
      parentProjectId
    )
  }
}
