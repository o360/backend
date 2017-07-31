ALTER TABLE project ADD machine_name VARCHAR;
UPDATE project SET machine_name = md5(random()::text);
ALTER TABLE project ALTER COLUMN machine_name SET NOT NULL;

ALTER TABLE form ADD machine_name VARCHAR;
UPDATE form SET machine_name = md5(random()::text);
ALTER TABLE form ALTER COLUMN machine_name SET NOT NULL;

ALTER TABLE form_answer ADD project_machine_name VARCHAR DEFAULT '' NOT NULL;
ALTER TABLE form_answer ADD form_machine_name VARCHAR DEFAULT '' NOT NULL;

UPDATE form_answer
SET project_machine_name = (SELECT machine_name FROM project WHERE form_answer.project_id = project.id)
WHERE project_id IS NOT NULL;

UPDATE form_answer
SET form_machine_name = (SELECT machine_name FROM form WHERE form_answer.form_id = form.id);
