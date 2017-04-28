CREATE TABLE event
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NOT NULL,
  description VARCHAR(1024) DEFAULT NULL,
  can_revote BOOLEAN DEFAULT FALSE  NOT NULL
);

CREATE TABLE event_notification
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  event_id BIGINT NOT NULL,
  time TIMESTAMP NOT NULL,
  kind SMALLINT NOT NULL,
  recipient_kind SMALLINT NOT NULL,
  CONSTRAINT event_notification_event_id_fk FOREIGN KEY (event_id)
    REFERENCES event (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE event_project
(
  event_id BIGINT NOT NULL,
  project_id BIGINT NOT NULL,
  CONSTRAINT event_project_event_id_project_id_pk PRIMARY KEY (event_id, project_id),
  CONSTRAINT event_project_event_id_fk FOREIGN KEY (event_id) REFERENCES event (id) ON UPDATE CASCADE,
  CONSTRAINT event_project_project_id_fk FOREIGN KEY (project_id) REFERENCES project (id) ON UPDATE CASCADE
);
