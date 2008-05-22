ALTER TABLE orders_ships ADD CONSTRAINT orders_ships_fk_ship_types FOREIGN KEY (type) REFERENCES ship_types(id);
