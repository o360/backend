CREATE TABLE active_project
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  event_id BIGINT NOT NULL,
  name VARCHAR(1024) NOT NULL,
  description VARCHAR(1024) NULL DEFAULT NULL,
  forms_on_same_page BOOLEAN NOT NULL,
  can_revote BOOLEAN NOT NULL,
  is_anonymous BOOLEAN NOT NULL,
  machine_name VARCHAR(1024) NOT NULL,
  parent_project_id BIGINT NULL,
  CONSTRAINT active_project_event_id_fk FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT active_project_project_id_fk FOREIGN KEY (parent_project_id) REFERENCES project (id) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE active_project_auditor
(
  active_project_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  CONSTRAINT active_project_active_project_id_user_id_pk PRIMARY KEY (active_project_id, user_id),
  CONSTRAINT active_project_active_project_id_fk FOREIGN KEY (active_project_id) REFERENCES active_project (id) ON UPDATE CASCADE,
  CONSTRAINT active_project_user_id_fk FOREIGN KEY (user_id) REFERENCES account (id) ON UPDATE CASCADE
);

ALTER TABLE form_answer ADD COLUMN active_project_id BIGINT NOT NULL;
ALTER TABLE form_answer ADD COLUMN status SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE form_answer DROP COLUMN event_id;
ALTER TABLE form_answer DROP COLUMN project_id;
ALTER TABLE form_answer DROP COLUMN project_machine_name;
ALTER TABLE form_answer DROP COLUMN form_machine_name;

