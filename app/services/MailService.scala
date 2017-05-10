package services

import javax.inject.{Inject, Singleton}

import models.user.User
import org.apache.commons.mail.EmailException
import play.api.Configuration
import play.api.libs.mailer.{Email, MailerClient}
import utils.Logger

/**
  * Mail service.
  */
@Singleton
class MailService @Inject()(
  protected val mailerClient: MailerClient,
  protected val configuration: Configuration
) extends Logger {

  /**
    * Sender address.
    */
  private def sendFromEmail = configuration.getString("play.mailer.from").get

  /**
    * Sends email.
    *
    * @param subject subject
    * @param to      recipient
    * @param text    body
    */
  def send(
    subject: String,
    to: User,
    text: String
  ) = {
    val email = Email(
      subject,
      sendFromEmail,
      Seq(s"${to.name.get} <${to.email.get}>"),
      bodyHtml = Some(text)
    )
    try {
      val eid = mailerClient.send(email)
      log.info(s"[email] message $eid sent to $to")
    } catch {
      case e: EmailException =>
        log.error("[email] sending failure", e)
    }
  }
}
