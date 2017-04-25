CREATE TABLE form
(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    name VARCHAR(1024) NOT NULL
);

CREATE TABLE form_element
(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    form_id BIGINT NOT NULL,
    kind SMALLINT NOT NULL,
    caption VARCHAR(1024) NOT NULL,
    default_value VARCHAR(1024),
    required BOOLEAN NOT NULL,
    ord INT NOT NULL,
    CONSTRAINT form_element_form_id_fk FOREIGN KEY (form_id) REFERENCES form (id)
      ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE form_element_value
(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    element_id BIGINT NOT NULL,
    value VARCHAR(1024) NOT NULL,
    caption VARCHAR(1024) NOT NULL,
    ord INT NOT NULL,
    CONSTRAINT form_element_value_form_element_id_fk FOREIGN KEY (element_id) REFERENCES form_element (id)
      ON DELETE CASCADE ON UPDATE CASCADE
);
