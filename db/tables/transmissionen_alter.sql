create index empfaenger on transmissionen (empfaenger, gelesen);
ALTER TABLE transmissionen ADD CONSTRAINT transmissionen_fk_users1 FOREIGN KEY (sender) REFERENCES users(id);
ALTER TABLE transmissionen ADD CONSTRAINT transmissionen_fk_users2 FOREIGN KEY (empfaenger) REFERENCES users(id);
