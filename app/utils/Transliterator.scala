package utils

import scala.annotation.tailrec

object Transliteration {

  private val nothing = "\\"

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
    "Ь" -> nothing,
    "Ъ" -> "IE",
    "Э" -> "E",
    "Ю" -> "IU",
    "Я" -> "IA"
  )

  private val rules = ruToEnRules ++ ruToEnRules.map(_.swap)

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
          case None => helper(rest.tail, acc + rest.head)
        }
      }

    helper(str.toUpperCase).toLowerCase.replace(nothing, "")
  }
}
