ALTER TABLE active_project_auditor DROP CONSTRAINT active_project_active_project_id_fk;

ALTER TABLE active_project_auditor
ADD CONSTRAINT active_project_active_project_id_fk
FOREIGN KEY (active_project_id)
REFERENCES active_project (id)
ON UPDATE CASCADE ON DELETE CASCADE;
