ALTER TABLE skn_visits ADD CONSTRAINT skn_visits_fk_users FOREIGN KEY (user) REFERENCES users(id);
ALTER TABLE skn_visits ADD CONSTRAINT skn_visits_fk_skn_channels FOREIGN KEY (channel) REFERENCES skn_channels(id);
