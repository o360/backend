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

CREATE TABLE event_form_mapping
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  event_id BIGINT NOT NULL,
  form_template_id BIGINT NOT NULL,
  form_freezed_id BIGINT NOT NULL,
  CONSTRAINT event_form_mapping_event_id_fk FOREIGN KEY (event_id)
    REFERENCES event (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT event_form_mapping_form_template_id_fk FOREIGN KEY (form_template_id)
    REFERENCES form (id) ON UPDATE CASCADE,
  CONSTRAINT event_form_mapping_form_freezed_id_fk FOREIGN KEY (form_freezed_id)
    REFERENCES form (id) ON DELETE CASCADE ON UPDATE CASCADE
);

ALTER TABLE form ADD kind SMALLINT DEFAULT 0 NOT NULL;
