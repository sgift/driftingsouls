ALTER TABLE ships_modules ADD CONSTRAINT ships_modules_fk_ships FOREIGN KEY (id) REFERENCES ships(id);
