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

import java.time.{LocalDate, LocalDateTime, LocalTime}

import org.scalacheck.{Arbitrary, Gen}

/**
  * Time generator for scalacheck.
  */
trait TimeGenerator {

  implicit val localTimeArb = Arbitrary {
    for {
      nanoOfDay <- Gen.chooseNum(LocalTime.MIN.toNanoOfDay, LocalTime.MAX.toNanoOfDay)
    } yield LocalTime.ofNanoOfDay(nanoOfDay)
  }

  implicit val localDateArb = Arbitrary {
    for {
      epochDay <- Gen.chooseNum(LocalDate.of(1970, 1, 1).toEpochDay, LocalDate.of(2100, 12, 31).toEpochDay)
    } yield LocalDate.ofEpochDay(epochDay)
  }

  implicit val localDateTimeArb = Arbitrary {
    for {
      date <- localDateArb.arbitrary
      time <- localTimeArb.arbitrary
    } yield LocalDateTime.of(date, time)
  }
}
