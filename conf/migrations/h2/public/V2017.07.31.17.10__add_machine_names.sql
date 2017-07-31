ALTER TABLE project ADD machine_name VARCHAR DEFAULT '';
ALTER TABLE form ADD machine_name VARCHAR DEFAULT '';
ALTER TABLE form_answer ADD project_machine_name VARCHAR DEFAULT '';
ALTER TABLE form_answer ADD form_machine_name VARCHAR DEFAULT '';
