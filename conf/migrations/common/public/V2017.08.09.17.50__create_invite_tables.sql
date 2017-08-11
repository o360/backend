CREATE TABLE invite
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  code VARCHAR(1024) NOT NULL,
  email VARCHAR(255) NOT NULL,
  creation_time TIMESTAMP NOT NULL,
  activation_time TIMESTAMP NULL,
  CONSTRAINT invite_code_unique UNIQUE (code)

);
CREATE INDEX invite_code ON invite (code);

CREATE TABLE invite_group
(
  invite_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  CONSTRAINT invite_group_invite_id_group_id_pk PRIMARY KEY (invite_id, group_id),
  CONSTRAINT invite_group_invite_id_fk FOREIGN KEY (invite_id) REFERENCES invite (id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT invite_group_group_id_fk FOREIGN KEY (group_id) REFERENCES orgstructure (id) ON UPDATE CASCADE ON DELETE CASCADE
);
