ALTER TABLE versteigerungen ADD CONSTRAINT versteigerungen_fk_users FOREIGN KEY (bieter) REFERENCES users(id);
