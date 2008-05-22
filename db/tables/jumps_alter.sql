ALTER TABLE jumps ADD CONSTRAINT jumps_fk_ships FOREIGN KEY (shipid) REFERENCES ships(id);
