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

CREATE TABLE form
(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    name VARCHAR(1024) NOT NULL
);

CREATE TABLE form_element
(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    form_id BIGINT NOT NULL,
    kind SMALLINT NOT NULL,
    caption VARCHAR(1024) NOT NULL,
    default_value VARCHAR(1024),
    required BOOLEAN NOT NULL,
    ord INT NOT NULL,
    CONSTRAINT form_element_form_id_fk FOREIGN KEY (form_id) REFERENCES form (id)
      ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE form_element_value
(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    element_id BIGINT NOT NULL,
    value VARCHAR(1024) NOT NULL,
    caption VARCHAR(1024) NOT NULL,
    ord INT NOT NULL,
    CONSTRAINT form_element_value_form_element_id_fk FOREIGN KEY (element_id) REFERENCES form_element (id)
      ON DELETE CASCADE ON UPDATE CASCADE
);
