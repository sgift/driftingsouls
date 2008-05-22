ALTER TABLE stats_module_locations ADD CONSTRAINT stats_module_locations_fk_user_id FOREIGN KEY (user_id) REFERENCES users(id);
