ALTER TABLE skn ADD CONSTRAINT skn_fk_skn_channels FOREIGN KEY (channel) REFERENCES skn_channels(id);
ALTER TABLE skn ADD CONSTRAINT skn_fk_users FOREIGN KEY (userid) REFERENCES users(id);
