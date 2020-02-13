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

CREATE TABLE project_email_template
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  project_id BIGINT NOT NULL,
  template_id BIGINT NOT NULL,
  kind SMALLINT NOT NULL,
  recipient_kind SMALLINT NOT NULL,
  CONSTRAINT project_email_template_project_id_fk FOREIGN KEY (project_id)
    REFERENCES project (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT project_email_template_template_id_fk FOREIGN KEY (template_id)
    REFERENCES template (id) ON UPDATE CASCADE
);

CREATE TABLE relation_email_template
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  relation_id BIGINT NOT NULL,
  template_id BIGINT NOT NULL,
  kind SMALLINT NOT NULL,
  recipient_kind SMALLINT NOT NULL,
  CONSTRAINT relation_email_template_relation_id_fk FOREIGN KEY (relation_id)
    REFERENCES relation (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT relation_email_template_template_id_fk FOREIGN KEY (template_id)
    REFERENCES template (id) ON UPDATE CASCADE
);
