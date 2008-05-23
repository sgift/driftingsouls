ALTER TABLE fz ADD CONSTRAINT fz_fk_bases FOREIGN KEY (col) REFERENCES bases(id);
ALTER TABLE fz ADD CONSTRAINT fz_fk_forschungen FOREIGN KEY (forschung) REFERENCES forschungen(id);
