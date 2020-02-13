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

/**
  * User groups fixture.
  */
trait UserGroupFixture extends FixtureHelper with UserFixture with GroupFixture { self: FixtureSupport =>

  val UserGroups = Seq(
    (1, 1),
    (1, 2),
    (2, 3),
    (2, 5)
  )

  addFixtureOperation {
    val builder = insertInto("user_group")
      .columns("user_id", "group_id")

    UserGroups.foreach { x =>
      builder.scalaValues(x._1, x._2)
    }

    builder.build
  }
}
