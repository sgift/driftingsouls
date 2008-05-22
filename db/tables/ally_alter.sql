ALTER TABLE ally ADD CONSTRAINT ally_fk_users FOREIGN KEY (president) REFERENCES users(id);
