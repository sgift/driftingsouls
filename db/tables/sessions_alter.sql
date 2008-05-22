ALTER TABLE sessions ADD CONSTRAINT sessions_fk_users FOREIGN KEY (id) REFERENCES users(id);
