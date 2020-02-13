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

import scala.annotation.tailrec

object Transliterator {

  private val ruToEnRules = Seq(
    "А" -> "A",
    "Б" -> "B",
    "В" -> "V",
    "Г" -> "G",
    "Д" -> "D",
    "Е" -> "E",
    "Ё" -> "E",
    "Ж" -> "ZH",
    "З" -> "Z",
    "И" -> "I",
    "Й" -> "I",
    "К" -> "K",
    "Л" -> "L",
    "М" -> "M",
    "Н" -> "N",
    "О" -> "O",
    "П" -> "P",
    "Р" -> "R",
    "С" -> "S",
    "Т" -> "T",
    "У" -> "U",
    "Ф" -> "F",
    "Х" -> "KH",
    "Ц" -> "TS",
    "Ч" -> "CH",
    "Ш" -> "SH",
    "Щ" -> "SHCH",
    "Ы" -> "Y",
    "Ь" -> "",
    "Ъ" -> "IE",
    "Э" -> "E",
    "Ю" -> "IU",
    "Я" -> "IA"
  )

  private val rules = ruToEnRules ++ ruToEnRules.map(_.swap).filter(_._1.nonEmpty)

  /**
    * Transliterates given string from russian to english and backwards.
    */
  def transliterate(str: String): String = {

    @tailrec
    def helper(rest: String, acc: String = ""): String =
      if (rest.isEmpty) acc
      else {
        rules
          .filter {
            case (from, _) =>
              rest.startsWith(from)
          }
          .sortBy { case (from, _) => -from.length }
          .headOption match {
          case Some((from, to)) => helper(rest.drop(math.max(from.length, 1)), acc + to)
          case None             => helper(rest.tail, acc + rest.head)
        }
      }

    helper(str.toUpperCase).toLowerCase
  }
}
