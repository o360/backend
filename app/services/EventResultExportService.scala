package services

import java.util.UUID
import javax.inject.Inject

import models.ListWithTotal
import models.assessment.UserAnswer
import models.dao._
import models.event.Event
import models.form.Form
import models.user.User
import play.api.libs.concurrent.Execution.Implicits._
import utils.RandomGenerator

import scala.concurrent.Future

/**
  * Service for exporting event results.
  */
class EventResultExportService @Inject()(
  answerDao: AnswerDao,
  formDao: FormDao,
  userDao: UserDao,
  groupDao: GroupDao,
  relationDao: ProjectRelationDao,
  projectDao: ProjectDao,
  eventDao: EventDao
) {

  type ExportResult = (Seq[Form], Seq[User], Seq[UserAnswer])

  def eventsList: Future[ListWithTotal[Event]] = eventDao.getList()

  def exportAnswers(eventId: Long): Future[ExportResult] = {

    def getUsers(answers: Seq[UserAnswer]) = {
      val userFromIds = answers.filter(!_.isAnonymous).map(_.userFromId)
      val userToIds = answers.flatMap(_.userTo)
      val ids = (userFromIds ++ userToIds).distinct
      userDao.getList(optIds = Some(ids), includeDeleted = true).map(_.data)
    }

    def anonimyzeAnswers(answers: Seq[UserAnswer], usersMap: Map[Long, String] = Map()): Seq[UserAnswer] = answers match {
      case Seq() => Seq()
      case answer +: tail if answer.isAnonymous && usersMap.contains(answer.userFromId) =>
        answer.copy(userFrom = usersMap(answer.userFromId)) +: anonimyzeAnswers(tail, usersMap)
      case answer +: tail if answer.isAnonymous =>
        anonimyzeAnswers(answers, usersMap + (answer.userFromId -> RandomGenerator.generateAnonymousUserName))
      case answer +: tail =>
        answer.copy(userFrom = answer.userFromId.toString) +: anonimyzeAnswers(tail, usersMap)
    }

    def getForms(answers: Seq[UserAnswer]) = {
      val ids = answers.map(_.answer.form.id).distinct
      for {
        forms <- Future.sequence(ids.map(formDao.findById))
      } yield forms.collect { case Some(form) => form }
    }

    for {
      answers <- answerDao.getEventAnswers(eventId)
      forms <- getForms(answers)
      users <- getUsers(answers)
    } yield (forms, users, anonimyzeAnswers(answers))
  }
}
