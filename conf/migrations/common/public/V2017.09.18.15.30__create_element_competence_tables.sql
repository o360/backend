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

CREATE TABLE form_element_competence
(
    element_id BIGINT NOT NULL,
    competence_id BIGINT NOT NULL,
    factor NUMERIC NOT NULL,
    CONSTRAINT form_element_competence_element_id_fk FOREIGN KEY (element_id)
        REFERENCES form_element (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT form_element_competence_competence_id_fk FOREIGN KEY (competence_id)
        REFERENCES competence (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT form_element_competence_element_id_competence_id_pk PRIMARY KEY (element_id, competence_id)
);
