CREATE TABLE user_meta
(
  user_id BIGINT NOT NULL,
  gdrive_folder_id VARCHAR DEFAULT NULL,
  CONSTRAINT user_meta_account_id_fk FOREIGN KEY (user_id) REFERENCES account (id) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE UNIQUE INDEX user_meta_user_id_uindex ON user_meta (user_id);
