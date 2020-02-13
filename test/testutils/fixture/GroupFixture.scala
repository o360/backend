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
import models.group.{Group => GroupModel}

/**
  * Groups fixture.
  */
trait GroupFixture extends FixtureHelper { self: FixtureSupport =>

  val Groups = GroupFixture.values

  addFixtureOperation {
    val builder = insertInto("orgstructure")
      .columns("id", "parent_id", "name")

    Groups.foreach { g =>
      builder.scalaValues(g.id, g.parentId.orNull, g.name)
    }

    builder.build
  }
}

object GroupFixture {
  val values = Seq(
    GroupModel(1, None, "1", hasChildren = false, level = 0),
    GroupModel(2, None, "2", hasChildren = true, level = 0),
    GroupModel(3, Some(2), "2-1", hasChildren = false, level = 0), // actual level is bigger, h2 limitations
    GroupModel(4, Some(2), "2-2", hasChildren = true, level = 0),
    GroupModel(5, Some(4), "2-2-1", hasChildren = false, level = 0)
  )
}
