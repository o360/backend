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

import javax.inject.{Inject, Singleton}

import models.user.User
import org.apache.commons.mail.EmailException
import play.api.libs.mailer.{Email, MailerClient}
import utils.{Config, Logger}

/**
  * Mail service.
  */
@Singleton
class MailService @Inject() (
  protected val mailerClient: MailerClient,
  protected val config: Config
) extends Logger {

  /**
    * Sender address.
    */
  private def sendFromEmail = config.mailerSettings.sendFrom

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
  ): Unit = {
    val name = to.fullName.getOrElse(throw new NoSuchElementException(s"name not defined, id ${to.id}"))
    val address = to.email.getOrElse(throw new NoSuchElementException(s"email not defined, id ${to.id}"))

    send(subject, name, address, text)
  }

  /**
    * Sends email.
    */
  def send(
    subject: String,
    name: String,
    address: String,
    text: String
  ): Unit = {

    val email = Email(
      subject,
      sendFromEmail,
      Seq(s"$name <$address>"),
      bodyHtml = Some(text)
    )
    log.trace(s"sending email $email")
    try {
      val eid = mailerClient.send(email)
      log.info(s"[email] message $eid sent to $address")
    } catch {
      case e: EmailException =>
        log.error("[email] sending failure", e)
    }
  }
}
