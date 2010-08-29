ALTER TABLE werften ADD CONSTRAINT werften_fk_bases FOREIGN KEY (linked) REFERENCES bases(id);
ALTER TABLE werften ADD CONSTRAINT werften_fk_ships FOREIGN KEY (shipid) REFERENCES ships(id);
ALTER TABLE werften ADD CONSTRAINT werften_fk_werften FOREIGN KEY (linkedWerft) REFERENCES werften(id) ON DELETE SET NULL;
