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

ALTER TABLE event_project DROP CONSTRAINT event_project_event_id_fk;
ALTER TABLE event_project
  ADD CONSTRAINT event_project_event_id_fk
FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE user_group DROP CONSTRAINT user_group_account_id_fk;
ALTER TABLE user_group
  ADD CONSTRAINT user_group_account_id_fk
FOREIGN KEY (user_id) REFERENCES account (id) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE orgstructure DROP CONSTRAINT orgstructure_orgstructure_id_fk;
ALTER TABLE orgstructure
  ADD CONSTRAINT orgstructure_orgstructure_id_fk
FOREIGN KEY (parent_id) REFERENCES orgstructure (id) ON DELETE CASCADE ON UPDATE CASCADE;
