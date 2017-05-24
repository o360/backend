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
    val name = to.name.getOrElse(throw new NoSuchElementException(s"name not defined, id ${to.id}"))
    val address = to.email.getOrElse(throw new NoSuchElementException(s"email not defined, id ${to.id}"))

    val email = Email(
      subject,
      sendFromEmail,
      Seq(s"$name <$address>"),
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
