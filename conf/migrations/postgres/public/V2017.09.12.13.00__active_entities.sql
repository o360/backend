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

CREATE TABLE active_project
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  event_id BIGINT NOT NULL,
  name VARCHAR(1024) NOT NULL,
  description VARCHAR(1024) NULL DEFAULT NULL,
  forms_on_same_page BOOLEAN NOT NULL,
  can_revote BOOLEAN NOT NULL,
  is_anonymous BOOLEAN NOT NULL,
  machine_name VARCHAR(1024) NOT NULL,
  parent_project_id BIGINT NULL,
  CONSTRAINT active_project_event_id_fk FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT active_project_project_id_fk FOREIGN KEY (parent_project_id) REFERENCES project (id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE active_project_auditor
(
  active_project_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  CONSTRAINT active_project_active_project_id_user_id_pk PRIMARY KEY (active_project_id, user_id),
  CONSTRAINT active_project_active_project_id_fk FOREIGN KEY (active_project_id) REFERENCES active_project (id) ON UPDATE CASCADE,
  CONSTRAINT active_project_user_id_fk FOREIGN KEY (user_id) REFERENCES account (id) ON UPDATE CASCADE
);

INSERT INTO active_project (event_id, name, description, forms_on_same_page, can_revote, is_anonymous, machine_name, parent_project_id)
SELECT e.id, p.name, p.description, p.forms_on_same_page, p.can_revote, p.is_anonymous, p.machine_name, p.id
FROM project p, event e
WHERE
EXISTS(SELECT 1 FROM event_project ep WHERE ep.event_id = e.id AND ep.project_id = p.id)
AND EXISTS (SELECT 1 FROM form_answer fa WHERE fa.event_id = e.id AND fa.project_id = p.id);

ALTER TABLE form_answer ADD COLUMN active_project_id BIGINT;
ALTER TABLE form_answer ADD COLUMN status SMALLINT NOT NULL DEFAULT 0;

UPDATE form_answer
SET status = 1;

UPDATE form_answer fa
SET active_project_id = ap.id
FROM (SELECT * FROM active_project) ap
WHERE ap.event_id = fa.event_id AND ap.parent_project_id = fa.project_id;

DELETE FROM form_answer WHERE active_project_id IS NULL;
ALTER TABLE form_answer ALTER COLUMN active_project_id SET NOT NULL;

ALTER TABLE form_answer DROP COLUMN event_id;
ALTER TABLE form_answer DROP COLUMN project_id;
ALTER TABLE form_answer DROP COLUMN project_machine_name;
ALTER TABLE form_answer DROP COLUMN form_machine_name;

INSERT INTO active_project_auditor (user_id, active_project_id)
SELECT a.id, ap.id
FROM account a, active_project ap, user_group ug, project p
WHERE ap.parent_project_id = p.id AND p.group_auditor_id = ug.group_id AND a.id = ug.user_id;
