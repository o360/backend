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

ALTER TABLE project ADD machine_name VARCHAR;
UPDATE project SET machine_name = md5(random()::text);
ALTER TABLE project ALTER COLUMN machine_name SET NOT NULL;

ALTER TABLE form ADD machine_name VARCHAR;
UPDATE form SET machine_name = md5(random()::text);
ALTER TABLE form ALTER COLUMN machine_name SET NOT NULL;

ALTER TABLE form_answer ADD project_machine_name VARCHAR DEFAULT '' NOT NULL;
ALTER TABLE form_answer ADD form_machine_name VARCHAR DEFAULT '' NOT NULL;

UPDATE form_answer
SET project_machine_name = (SELECT machine_name FROM project WHERE form_answer.project_id = project.id)
WHERE project_id IS NOT NULL;

UPDATE form_answer
SET form_machine_name = (SELECT machine_name FROM form WHERE form_answer.form_id = form.id);
