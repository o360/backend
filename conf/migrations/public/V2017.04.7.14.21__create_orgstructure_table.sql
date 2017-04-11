CREATE TABLE orgstructure
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  parent_id BIGINT DEFAULT NULL,
  name VARCHAR(1024) NOT NULL,
  CONSTRAINT orgstructure_orgstructure_id_fk FOREIGN KEY (parent_id)
    REFERENCES orgstructure (id) ON UPDATE CASCADE
);

CREATE INDEX orgstructure_parent_id_index ON orgstructure (parent_id);
