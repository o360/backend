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

import models.ListWithTotal
import org.scalacheck.{Arbitrary, Gen}

/**
  * ListWithTotal generator for scalacheck.
  */
trait ListWithTotalGenerator {

  implicit def listWithTotalArb[A](implicit arbitrary: Arbitrary[A]): Arbitrary[ListWithTotal[A]] = Arbitrary {
    for {
      values <- Arbitrary.arbitrary[Seq[A]]
      total <- Gen.choose(values.length, Int.MaxValue)
    } yield ListWithTotal(total, values)
  }
}
