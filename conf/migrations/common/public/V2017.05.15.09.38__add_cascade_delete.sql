ALTER TABLE event_project DROP CONSTRAINT event_project_event_id_fk;
ALTER TABLE event_project
  ADD CONSTRAINT event_project_event_id_fk
FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE user_group DROP CONSTRAINT user_group_account_id_fk;
ALTER TABLE user_group
  ADD CONSTRAINT user_group_account_id_fk
FOREIGN KEY (user_id) REFERENCES account (id) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE orgstructure DROP CONSTRAINT orgstructure_orgstructure_id_fk;
ALTER TABLE orgstructure
  ADD CONSTRAINT orgstructure_orgstructure_id_fk
FOREIGN KEY (parent_id) REFERENCES orgstructure (id) ON DELETE CASCADE ON UPDATE CASCADE;
