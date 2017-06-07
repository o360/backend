package services

import java.io.FileInputStream
import java.time.format.DateTimeFormatter
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
      "user_name" -> recipient.name.getOrElse(""),
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

