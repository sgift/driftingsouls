ALTER TABLE bases ADD CONSTRAINT bases_fk_users FOREIGN KEY (owner) REFERENCES users(id);
