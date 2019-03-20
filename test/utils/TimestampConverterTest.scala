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
