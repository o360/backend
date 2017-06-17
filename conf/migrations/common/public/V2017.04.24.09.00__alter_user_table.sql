ALTER TABLE account ADD gender SMALLINT DEFAULT NULL NULL;

UPDATE account SET gender = 0 WHERE status <> 0;
