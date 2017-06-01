CREATE TABLE event_job
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  event_id BIGINT NOT NULL,
  time TIMESTAMP NOT NULL,
  status SMALLINT NOT NULL,
  notification_kind SMALLINT,
  notification_recipient_kind SMALLINT,
  job_type SMALLINT NOT NULL,
  CONSTRAINT event_job_event_id_fk FOREIGN KEY (event_id) REFERENCES event (id) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX event_job_time_index ON event_job (time);
CREATE INDEX event_job_status_index ON event_job (status);
