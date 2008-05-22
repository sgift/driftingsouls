ALTER TABLE battles_ships ADD CONSTRAINT battles_ships_fk_battles FOREIGN KEY (battleid) REFERENCES battles(id);
ALTER TABLE battles_ships ADD CONSTRAINT battles_ships_fk_ships FOREIGN KEY (shipid) REFERENCES ships(id);
