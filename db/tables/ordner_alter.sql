ALTER TABLE ordner ADD CONSTRAINT ordner_fk_users FOREIGN KEY (playerid) REFERENCES users(id);
