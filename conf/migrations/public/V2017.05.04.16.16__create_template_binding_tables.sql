CREATE TABLE project_email_template
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  project_id BIGINT NOT NULL,
  template_id BIGINT NOT NULL,
  kind SMALLINT NOT NULL,
  recipient_kind SMALLINT NOT NULL,
  CONSTRAINT project_email_template_project_id_fk FOREIGN KEY (project_id)
    REFERENCES project (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT project_email_template_template_id_fk FOREIGN KEY (template_id)
    REFERENCES template (id) ON UPDATE CASCADE
);

CREATE TABLE relation_email_template
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  relation_id BIGINT NOT NULL,
  template_id BIGINT NOT NULL,
  kind SMALLINT NOT NULL,
  recipient_kind SMALLINT NOT NULL,
  CONSTRAINT relation_email_template_relation_id_fk FOREIGN KEY (relation_id)
    REFERENCES relation (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT relation_email_template_template_id_fk FOREIGN KEY (template_id)
    REFERENCES template (id) ON UPDATE CASCADE
);
