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

package utils
import java.time.{LocalDateTime, ZoneId, ZoneOffset}

import services.BaseServiceTest
import testutils.generator.TimeGenerator

class TimestampConverterTest extends BaseServiceTest with TimeGenerator {

  "fromUtc" should {

    "do nothing when target timezone is UTC" in {
      forAll { dateTime: LocalDateTime =>
        val result = TimestampConverter.fromUtc(dateTime, ZoneOffset.UTC)
        result mustBe dateTime
      }
    }

    "convert from UTC to GMT+7 timezone" in {
      val result = TimestampConverter.fromUtc(LocalDateTime.of(2010, 6, 2, 3, 50), ZoneId.of("GMT+7"))
      result mustBe LocalDateTime.of(2010, 6, 2, 10, 50)
    }

  }

  "toUtc" should {

    "do nothing when target timezone is UTC" in {
      forAll { dateTime: LocalDateTime =>
        val result = TimestampConverter.toUtc(dateTime, ZoneOffset.UTC)
        result mustBe dateTime
      }
    }

    "convert to UTC from GMT+7 timezone" in {
      val result = TimestampConverter.toUtc(LocalDateTime.of(2010, 6, 2, 14, 50), ZoneId.of("GMT+7"))
      result mustBe LocalDateTime.of(2010, 6, 2, 7, 50)
    }

  }

  "toPrettyString" should {

    "print date" in {
      val dateTime = LocalDateTime.of(2010, 6, 2, 7, 50)
      val zone = ZoneId.of("Asia/Tomsk")

      val result = TimestampConverter.toPrettyString(dateTime, zone)

      result mustBe "2010-06-02 07:50 (Asia/Tomsk)"
    }

  }

}
