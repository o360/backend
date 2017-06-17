-- h2 doesn't support views with recursive queries
CREATE VIEW orgstructure_level_view AS SELECT id as group_id, 0 as lvl from orgstructure
