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

import models.group.Group
import org.scalacheck.Arbitrary

/**
  * Group generator for scalacheck.
  */
trait GroupGenerator {

  implicit val groupArbitrary = Arbitrary {
    for {
      id <- Arbitrary.arbitrary[Long]
      parentId <- Arbitrary.arbitrary[Option[Long]]
      name <- Arbitrary.arbitrary[String]
      hasChildren <- Arbitrary.arbitrary[Boolean]
      level <- Arbitrary.arbitrary[Int]
    } yield Group(id, parentId, name, hasChildren, level)
  }
}
