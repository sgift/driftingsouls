ALTER TABLE handel ADD CONSTRAINT handel_fk_users FOREIGN KEY (who) REFERENCES users(id);
