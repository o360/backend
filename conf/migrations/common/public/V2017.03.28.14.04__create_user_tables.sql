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

CREATE TABLE account
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  name VARCHAR(1024) DEFAULT NULL,
  email VARCHAR(255) DEFAULT NULL,
  role SMALLINT DEFAULT 0 NOT NULL,
  status SMALLINT DEFAULT 0 NOT NULL
);

CREATE TABLE user_login
(
  user_id BIGINT NOT NULL,
  provider_id VARCHAR(255) NOT NULL,
  provider_key VARCHAR(1024) NOT NULL,
  CONSTRAINT user_login_account_id_fk FOREIGN KEY (user_id) REFERENCES account (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT user_login_user_id_provider_id_provider_key_pk UNIQUE (provider_id, provider_key)
);
