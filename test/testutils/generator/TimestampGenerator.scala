package testutils.generator

import java.sql.Timestamp

import org.scalacheck.{Arbitrary, Gen}

/**
  * Timestamp generator for scalacheck.
  */
trait TimestampGenerator {
    implicit val timestampArb = Arbitrary {
      Gen.choose(0L, 253402300799L).map(new Timestamp(_)) // max year - 9999
    }
  }
