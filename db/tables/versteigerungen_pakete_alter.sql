ALTER TABLE versteigerungen_pakete ADD CONSTRAINT versteigerungen_pakete_fk_users FOREIGN KEY (bieter) REFERENCES users(id);
