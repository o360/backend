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

CREATE TABLE competence_group
(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    name VARCHAR(1024) NOT NULL,
    description VARCHAR(1024) NULL DEFAULT NULL
);

CREATE TABLE competence
(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    group_id BIGINT NOT NULL,
    name VARCHAR(1024) NOT NULL,
    description VARCHAR(1024) NULL DEFAULT NULL,
    CONSTRAINT competence_group_id_fk FOREIGN KEY (group_id) REFERENCES competence_group(id) ON UPDATE CASCADE
);
