package services

import java.time.format.DateTimeFormatter
import javax.inject.Singleton

import models.event.Event
import models.user.User

import scala.util.matching.Regex.Match


/**
  * Template engine service
  */
@Singleton
class TemplateEngineService {

  private val templateRegex = """\{\{\s*([a-zA-Z]+)\s*\}\}""".r

  /**
    * Render template using context.
    */
  def render(template: String, context: Map[String, String]): String = {

    def getReplacement(m: Match) = context.get(m.group(1)).map(_.toString).getOrElse("")

    templateRegex.replaceAllIn(template, getReplacement _)
  }

  /**
    * Returns context for given arguments.
    */
  def getContext(
    recipient: User,
    event: Event
  ): Map[String, String] = {
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    Map(
      "user_name" -> recipient.name.getOrElse(""),
      "event_start" -> event.start.toLocalDateTime.format(dateTimeFormatter),
      "event_end" -> event.end.toLocalDateTime.format(dateTimeFormatter),
      "event_description" -> event.description.getOrElse("")
    )
  }
}

