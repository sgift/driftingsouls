ALTER TABLE stats_user_cargo ADD CONSTRAINT stats_user_cargo_fk_user_id FOREIGN KEY (user_id) REFERENCES users(id);
