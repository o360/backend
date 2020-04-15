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

package services

import java.io.FileInputStream
import javax.inject.Singleton

import models.event.Event
import models.user.User
import utils.TimestampConverter

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

/**
  * Template engine service
  */
@Singleton
class TemplateEngineService {

  private val templateRegex = """\{\{\s*([a-zA-Z_]+)\s*\}\}""".r

  /**
    * Render template using context.
    */
  def render(template: String, context: Map[String, String]): String = {

    def getReplacement(m: Match) = Regex.quoteReplacement(context.get(m.group(1)).map(_.toString).getOrElse(""))

    templateRegex.replaceAllIn(template, getReplacement _)
  }

  /**
    * Returns context for given arguments.
    */
  def getContext(
    recipient: User,
    event: Option[Event]
  ): Map[String, String] = {
    Map(
      "user_name" -> recipient.fullName.getOrElse(""),
      "event_start" -> event.fold("")(e => TimestampConverter.toPrettyString(e.start, recipient.timezone)),
      "event_end" -> event.fold("")(e => TimestampConverter.toPrettyString(e.end, recipient.timezone)),
      "event_description" -> event.fold("")(_.description.getOrElse(""))
    )
  }

  /**
    * Load template from templates folder.
    */
  def loadStaticTemplate(name: String): String = {
    val stream = new FileInputStream(s"templates/$name")
    scala.io.Source.fromInputStream(stream).getLines.mkString(System.lineSeparator)
  }
}
