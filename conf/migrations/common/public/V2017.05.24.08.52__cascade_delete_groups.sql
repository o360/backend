ALTER TABLE user_group DROP CONSTRAINT user_group_orgstructure_id_fk;
ALTER TABLE user_group
  ADD CONSTRAINT user_group_orgstructure_id_fk
  FOREIGN KEY (group_id) REFERENCES orgstructure (id) ON DELETE CASCADE ON UPDATE CASCADE;
