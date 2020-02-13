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

package testutils.fixture

import com.ninja_squad.dbsetup.Operations._
import models.NamedEntity
import models.notification._
import models.project.{Relation, TemplateBinding}

/**
  * Project relation fixture.
  */
trait ProjectRelationFixture
  extends FixtureHelper
  with ProjectFixture
  with GroupFixture
  with FormFixture
  with TemplateFixture { self: FixtureSupport =>

  val ProjectRelations = ProjectRelationFixture.values

  addFixtureOperation {
    sequenceOf(
      insertInto("relation")
        .columns(
          "id",
          "project_id",
          "group_from_id",
          "group_to_id",
          "form_id",
          "kind",
          "can_self_vote",
          "can_skip_answers"
        )
        .scalaValues(1, 1, 1, 2, 1, 0, true, true)
        .scalaValues(2, 1, 2, null, 2, 1, false, false)
        .build,
      insertInto("relation_email_template")
        .columns("relation_id", "template_id", "kind", "recipient_kind")
        .scalaValues(1, 1, 1, 0)
        .scalaValues(1, 2, 3, 0)
        .scalaValues(1, 2, 3, 1)
        .build
    )
  }
}

object ProjectRelationFixture {

  val classicRelation = Relation(
    id = 1,
    project = NamedEntity(1, ProjectFixture.values.find(_.id == 1).get.name),
    groupFrom = NamedEntity(1, GroupFixture.values.find(_.id == 1).get.name),
    groupTo = Some(NamedEntity(2, GroupFixture.values.find(_.id == 2).get.name)),
    form = NamedEntity(1, FormFixture.values.find(_.id == 1).get.name),
    kind = Relation.Kind.Classic,
    templates = Seq(
      TemplateBinding(NamedEntity(1, "firstname"), Begin, Respondent),
      TemplateBinding(NamedEntity(2, "secondname"), End, Respondent),
      TemplateBinding(NamedEntity(2, "secondname"), End, Auditor)
    ),
    hasInProgressEvents = false,
    canSelfVote = true,
    canSkipAnswers = true
  )

  val surveyRelation = Relation(
    id = 2,
    project = NamedEntity(1, ProjectFixture.values.find(_.id == 1).get.name),
    groupFrom = NamedEntity(2, GroupFixture.values.find(_.id == 2).get.name),
    groupTo = None,
    form = NamedEntity(2, FormFixture.values.find(_.id == 2).get.name),
    kind = Relation.Kind.Survey,
    templates = Nil,
    hasInProgressEvents = false,
    canSelfVote = false,
    canSkipAnswers = false
  )
  val values = Seq(
    classicRelation,
    surveyRelation
  )
}
