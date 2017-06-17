CREATE TABLE user_group
(
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    CONSTRAINT user_group_user_id_group_id_pk PRIMARY KEY (user_id, group_id),
    CONSTRAINT user_group_account_id_fk FOREIGN KEY (user_id) REFERENCES account (id) ON UPDATE CASCADE,
    CONSTRAINT user_group_orgstructure_id_fk FOREIGN KEY (group_id) REFERENCES orgstructure (id) ON UPDATE CASCADE
);
