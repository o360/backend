package utils

import org.scalatestplus.play.PlaySpec

/**
  * Test for transliteration.
  */
class TransliterationTest extends PlaySpec {

  "transliterate" should {
    "translit ru to en" in {
      val input = "южно-эфиопский грач увёл мышь за хобот на съезд ящериц."
      val expectedResult = "iuzhno-efiopskii grach uvel mysh za khobot na sieezd iashcherits."

      val result = Transliterator.transliterate(input)

      result mustBe expectedResult
    }

    "translit en to ru" in {
      val input = "v chаshchаkh iugа zhil-byl tsitrus... — dа, no fаlshivyi ekzempliarie!"
      val expectedResult = "в чaщaх югa жил-был цитрус... — дa, но фaлшивыи екземпляръ!"

      val result = Transliterator.transliterate(input)

      result mustBe expectedResult
    }

    "translit special cases" in {
      val input = "\\"

      val result = Transliterator.transliterate(input)

      result mustBe input
    }
  }

}
