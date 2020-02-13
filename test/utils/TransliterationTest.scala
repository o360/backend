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
