ALTER TABLE users ADD CONSTRAINT users_fk_ally FOREIGN KEY (ally) REFERENCES ally(id);
ALTER TABLE users ADD CONSTRAINT users_fk_ships FOREIGN KEY (flagschiff) REFERENCES ships(id);
ALTER TABLE users ADD CONSTRAINT users_fk_ally_posten FOREIGN KEY (allyposten) REFERENCES ally_posten(id);
