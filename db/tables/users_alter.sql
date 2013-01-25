ALTER TABLE users ADD CONSTRAINT users_fk_ally FOREIGN KEY (ally) REFERENCES ally(id);
ALTER TABLE users ADD CONSTRAINT users_fk_ally_posten FOREIGN KEY (allyposten) REFERENCES ally_posten(id);
