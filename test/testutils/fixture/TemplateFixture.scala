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
import models.template.Template
import models.notification._

/**
  * Templates fixture.
  */
trait TemplateFixture extends FixtureHelper { self: FixtureSupport =>

  val Templates = Seq(
    Template(1, "firstname", "firstsubject", "firstbody", PreBegin, Respondent),
    Template(2, "secondname", "secondsubject", "secondbody", End, Auditor),
    Template(3, "thirdname", "secondsubject", "thirdbody", Begin, Respondent)
  )

  addFixtureOperation {
    insertInto("template")
      .columns("id", "name", "subject", "body", "kind", "recipient_kind")
      .scalaValues(1, "firstname", "firstsubject", "firstbody", 0, 0)
      .scalaValues(2, "secondname", "secondsubject", "secondbody", 3, 1)
      .scalaValues(3, "thirdname", "secondsubject", "thirdbody", 1, 0)
      .build
  }
}
