ALTER TABLE sessions ADD CONSTRAINT sessions_fk_users FOREIGN KEY (userId) REFERENCES users(id);
