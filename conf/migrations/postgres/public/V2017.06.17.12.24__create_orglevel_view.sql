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
