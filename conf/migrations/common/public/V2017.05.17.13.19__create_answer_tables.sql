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

CREATE TABLE form_answer
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  event_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  user_from_id BIGINT NOT NULL,
  user_to_id BIGINT,
  form_id BIGINT NOT NULL,
  CONSTRAINT form_answer_event_id_fk FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT form_answer_project_id_fk FOREIGN KEY (project_id) REFERENCES project (id) ON UPDATE CASCADE,
  CONSTRAINT form_answer_account_id_from_fk FOREIGN KEY (user_from_id) REFERENCES account (id) ON UPDATE CASCADE,
  CONSTRAINT form_answer_account_id_to_fk FOREIGN KEY (user_to_id) REFERENCES account (id) ON UPDATE CASCADE,
  CONSTRAINT form_answer_form_id_fk FOREIGN KEY (form_id) REFERENCES form (id) ON UPDATE CASCADE
);

CREATE TABLE form_element_answer
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  answer_id BIGINT NOT NULL,
  form_element_id BIGINT NOT NULL,
  text VARCHAR,
  CONSTRAINT form_element_answer_form_answer_id_fk FOREIGN KEY (answer_id)
    REFERENCES form_answer (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT form_element_answer_form_element_id_fk FOREIGN KEY (form_element_id)
    REFERENCES form_element(id) ON UPDATE CASCADE
);

CREATE TABLE form_element_answer_value
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  answer_element_id BIGINT NOT NULL,
  form_element_value_id BIGINT NOT NULL,
  CONSTRAINT form_element_answer_value_form_element_answer_id_fk FOREIGN KEY (answer_element_id)
    REFERENCES form_element_answer (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT form_element_answer_value_form_element_value_id_fk FOREIGN KEY (form_element_value_id)
    REFERENCES form_element_value (id) ON UPDATE CASCADE
);
