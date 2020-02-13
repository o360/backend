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

package services.event

import javax.inject.Inject

import models.ListWithTotal
import models.assessment.Answer
import models.competence.{Competence, CompetenceGroup}
import models.dao._
import models.event.Event
import models.form.Form
import models.user.User

import scala.concurrent.{ExecutionContext, Future}

/**
  * Service for exporting event results.
  */
class EventResultExportService @Inject() (
  answerDao: AnswerDao,
  formDao: FormDao,
  userDao: UserDao,
  eventDao: EventDao,
  competenceDao: CompetenceDao,
  competenceGroupDao: CompetenceGroupDao,
  implicit val ec: ExecutionContext
) {

  type ExportResult = (Seq[Form], Seq[User], Seq[Answer], Seq[Competence], Seq[CompetenceGroup])

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

    def getCompetencies(forms: Seq[Form]) = {
      val ids = forms.flatMap(_.elements.flatMap(_.competencies)).map(_.competence.id).distinct
      competenceDao.getList(optIds = Some(ids)).map(_.data)
    }

    def getCompetenceGroups(s: Seq[Competence]) = {
      val ids = s.map(_.groupId).distinct
      competenceGroupDao.getList(optIds = Some(ids)).map(_.data)
    }

    for {
      answers <- answerDao.getList(optEventId = Some(eventId))
      forms <- getForms(answers)
      users <- getUsers(answers)
      competencies <- getCompetencies(forms)
      competenceGroups <- getCompetenceGroups(competencies)
    } yield (forms, users, anonimyzeAnswers(answers), competencies, competenceGroups)
  }
}
