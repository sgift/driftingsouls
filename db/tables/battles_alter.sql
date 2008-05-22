ALTER TABLE battles ADD CONSTRAINT battles_fk_users1 FOREIGN KEY (commander1) REFERENCES users(id);
ALTER TABLE battles ADD CONSTRAINT battles_fk_users2 FOREIGN KEY (commander2) REFERENCES users(id);
