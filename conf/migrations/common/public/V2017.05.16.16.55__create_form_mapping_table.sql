CREATE TABLE event_form_mapping
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  event_id BIGINT NOT NULL,
  form_template_id BIGINT NOT NULL,
  form_freezed_id BIGINT NOT NULL,
  CONSTRAINT event_form_mapping_event_id_fk FOREIGN KEY (event_id)
    REFERENCES event (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT event_form_mapping_form_template_id_fk FOREIGN KEY (form_template_id)
    REFERENCES form (id) ON UPDATE CASCADE,
  CONSTRAINT event_form_mapping_form_freezed_id_fk FOREIGN KEY (form_freezed_id)
    REFERENCES form (id) ON DELETE CASCADE ON UPDATE CASCADE
);

ALTER TABLE form ADD kind SMALLINT DEFAULT 0 NOT NULL;
