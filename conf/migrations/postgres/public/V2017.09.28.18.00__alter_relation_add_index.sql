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

CREATE UNIQUE INDEX relation_classic_unique ON relation (project_id, group_from_id, group_to_id, form_id) WHERE kind = 0;
CREATE UNIQUE INDEX relation_survey_unique ON relation (project_id, group_from_id, form_id) WHERE kind = 1;
