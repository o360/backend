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
import models.EntityKind
import models.competence.CompetenceGroup

/**
  * Competence group fixture.
  */
trait CompetenceGroupFixture extends FixtureHelper { self: FixtureSupport =>

  addFixtureOperation {
    insertInto("competence_group")
      .columns("id", "name", "description", "kind", "machine_name")
      .scalaValues(1, "comp group 1", "comp group 1 desc", 0, "comp group 1 machine name")
      .scalaValues(2, "comp group 2", null, 0, "comp group 2 machine name")
      .scalaValues(3, "comp group 1", "comp group 1 desc", 1, "comp group 1 machine name")
      .build
  }

  val CompetenceGroups = CompetenceGroupFixture.values
}

object CompetenceGroupFixture {
  val values = Seq(
    CompetenceGroup(
      id = 1,
      name = "comp group 1",
      description = Some("comp group 1 desc"),
      kind = EntityKind.Template,
      machineName = "comp group 1 machine name"
    ),
    CompetenceGroup(
      id = 2,
      name = "comp group 2",
      description = None,
      kind = EntityKind.Template,
      machineName = "comp group 2 machine name"
    ),
    CompetenceGroup(
      id = 3,
      name = "comp group 1",
      description = Some("comp group 1 desc"),
      kind = EntityKind.Freezed,
      machineName = "comp group 1 machine name"
    )
  )
}
