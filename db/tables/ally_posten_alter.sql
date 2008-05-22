ALTER TABLE ally_posten ADD CONSTRAINT ally_posten_fk_ally FOREIGN KEY (ally) REFERENCES ally(id);
