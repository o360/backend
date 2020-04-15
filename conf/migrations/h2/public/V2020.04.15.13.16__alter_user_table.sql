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

ALTER TABLE account ADD first_name VARCHAR(1024) DEFAULT NULL;
ALTER TABLE account ADD last_name VARCHAR(1024) DEFAULT NULL;

UPDATE account SET first_name = SUBSTRING(name, 0, LOCATE(' ', name) - 1);
UPDATE account SET last_name = SUBSTRING(name, LOCATE(' ', name) + 1);

ALTER TABLE account DROP COLUMN name;
