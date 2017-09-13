package services.event

import javax.inject.Inject

import models.ListWithTotal
import models.assessment.Answer
import models.dao._
import models.event.Event
import models.form.Form
import models.user.User

import scala.concurrent.{ExecutionContext, Future}

/**
  * Service for exporting event results.
  */
class EventResultExportService @Inject()(
  answerDao: AnswerDao,
  formDao: FormDao,
  userDao: UserDao,
  eventDao: EventDao,
  implicit val ec: ExecutionContext
) {

  type ExportResult = (Seq[Form], Seq[User], Seq[Answer])

  def eventsList: Future[ListWithTotal[Event]] = eventDao.getList()

  def exportAnswers(eventId: Long): Future[ExportResult] = {

    def getUsers(answers: Seq[Answer]) = {
      val userFromIds = answers.filterNot(_.isAnonymous).map(_.userFromId)
      val userToIds = answers.flatMap(_.userToId)
      val ids = (userFromIds ++ userToIds).distinct
      userDao.getList(optIds = Some(ids), includeDeleted = true).map(_.data)
    }

    def anonimyzeAnswers(answers: Seq[Answer], usersMap: Map[Long, Long] = Map()): Seq[Answer] =
      answers match {
        case Seq() => Seq()
        case answer +: tail if answer.isAnonymous && usersMap.contains(answer.userFromId) =>
          answer.copy(userFromId = usersMap(answer.userFromId)) +: anonimyzeAnswers(tail, usersMap)
        case answer +: _ if answer.isAnonymous =>
          anonimyzeAnswers(answers, usersMap + (answer.userFromId -> 0))
        case answer +: tail =>
          answer.copy(userFromId = answer.userFromId) +: anonimyzeAnswers(tail, usersMap)
      }

    def getForms(answers: Seq[Answer]) = {
      val ids = answers.map(_.form.id).distinct
      for {
        forms <- Future.sequence(ids.map(formDao.findById))
      } yield forms.collect { case Some(form) => form }
    }

    for {
      answers <- answerDao.getList(optEventId = Some(eventId))
      forms <- getForms(answers)
      users <- getUsers(answers)
    } yield (forms, users, anonimyzeAnswers(answers))
  }
}
