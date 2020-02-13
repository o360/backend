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

import com.ninja_squad.dbsetup.Operations.insertInto

/**
  * User login fixture.
  */
trait UserLoginFixture extends UserFixture with FixtureHelper { self: FixtureSupport =>

  val UserLogins = Seq(
    (1, "prov-id-1", "prov-key-1"),
    (1, "prov-id-2", "prov-key-2"),
    (2, "prov-id-1", "prov-key-3")
  )

  addFixtureOperation {
    val builder = insertInto("user_login")
      .columns("user_id", "provider_id", "provider_key")

    UserLogins.foreach {
      case (userId, provId, provKey) =>
        builder.scalaValues(userId, provId, provKey)
    }

    builder.build
  }
}
