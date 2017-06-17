ALTER TABLE form_answer ALTER COLUMN project_id DROP NOT NULL;

ALTER TABLE form_answer DROP CONSTRAINT form_answer_project_id_fk;
ALTER TABLE form_answer ADD CONSTRAINT form_answer_project_id_fk
  FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE SET NULL ON UPDATE CASCADE;
