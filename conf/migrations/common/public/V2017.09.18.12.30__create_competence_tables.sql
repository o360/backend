CREATE TABLE competence_group
(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    name VARCHAR(1024) NOT NULL,
    description VARCHAR(1024) NULL DEFAULT NULL
);

CREATE TABLE competence
(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    group_id BIGINT NOT NULL,
    name VARCHAR(1024) NOT NULL,
    description VARCHAR(1024) NULL DEFAULT NULL,
    CONSTRAINT competence_group_id_fk FOREIGN KEY (group_id) REFERENCES competence_group(id) ON UPDATE CASCADE
);
