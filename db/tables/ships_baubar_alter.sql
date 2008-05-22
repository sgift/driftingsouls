ALTER TABLE ships_baubar ADD CONSTRAINT ships_baubar_type_fk FOREIGN KEY (type) REFERENCES ship_types(id);
