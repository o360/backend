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

CREATE VIEW orgstructure_level_view AS
    WITH RECURSIVE T(id, lvl) AS (
        SELECT id, 0 AS lvl
        FROM orgstructure g
        UNION ALL
        SELECT gc.id, T.lvl + 1
        FROM T JOIN orgstructure gc
        ON gc.parent_id = T.id
    )
    SELECT id as group_id, MAX(lvl) as lvl
    FROM T
    GROUP BY id
