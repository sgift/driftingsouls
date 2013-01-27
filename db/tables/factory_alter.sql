ALTER TABLE factory ADD UNIQUE col_buildingid_idx (col, buildingid);
ALTER TABLE factory ADD CONSTRAINT factory_fk_bases FOREIGN KEY (col) REFERENCES bases(id);