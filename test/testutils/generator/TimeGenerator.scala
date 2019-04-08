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
