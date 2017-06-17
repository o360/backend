CREATE TABLE account
(
  id BIGSERIAL PRIMARY KEY NOT NULL,
  name VARCHAR(1024) DEFAULT NULL,
  email VARCHAR(255) DEFAULT NULL,
  role SMALLINT DEFAULT 0 NOT NULL,
  status SMALLINT DEFAULT 0 NOT NULL
);

CREATE TABLE user_login
(
  user_id BIGINT NOT NULL,
  provider_id VARCHAR(255) NOT NULL,
  provider_key VARCHAR(1024) NOT NULL,
  CONSTRAINT user_login_account_id_fk FOREIGN KEY (user_id) REFERENCES account (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT user_login_user_id_provider_id_provider_key_pk UNIQUE (provider_id, provider_key)
);
