CREATE TABLE project
(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    name VARCHAR(1024) NOT NULL,
    description VARCHAR(1024) NULL DEFAULT NULL,
    group_auditor_id BIGINT NOT NULL,
    CONSTRAINT project_group_auditor_id_fk FOREIGN KEY (group_auditor_id) REFERENCES orgstructure (id) ON UPDATE CASCADE
);

CREATE TABLE relation
(
    project_id BIGINT NOT NULL,
    group_from_id BIGINT NOT NULL,
    group_to_id BIGINT NOT NULL,
    form_id BIGINT NOT NULL,
    CONSTRAINT relation_project_id_fk FOREIGN KEY (project_id) REFERENCES project (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT relation_group_from_id_fk FOREIGN KEY (group_from_id) REFERENCES orgstructure (id) ON UPDATE CASCADE,
    CONSTRAINT relation_group_to_id_fk FOREIGN KEY (group_to_id) REFERENCES orgstructure (id) ON UPDATE CASCADE,
    CONSTRAINT relation_form_id_fk FOREIGN KEY (form_id) REFERENCES form (id) ON UPDATE CASCADE,
    CONSTRAINT relation_pk PRIMARY KEY (project_id, group_from_id, group_to_id, form_id)
);
