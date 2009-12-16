ALTER TABLE stats_user_unitcargo ADD CONSTRAINT stats_user_unitcargo_fk_user_id FOREIGN KEY (user_id) REFERENCES users(id);
