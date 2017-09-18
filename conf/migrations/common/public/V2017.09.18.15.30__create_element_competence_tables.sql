CREATE TABLE form_element_competence
(
    element_id BIGINT NOT NULL,
    competence_id BIGINT NOT NULL,
    factor NUMERIC NOT NULL,
    CONSTRAINT form_element_competence_element_id_fk FOREIGN KEY (element_id)
        REFERENCES form_element (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT form_element_competence_competence_id_fk FOREIGN KEY (competence_id)
        REFERENCES competence (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT form_element_competence_element_id_competence_id_pk PRIMARY KEY (element_id, competence_id)
);
