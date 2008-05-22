ALTER TABLE tradepost_sell ADD CONSTRAINT tradepost_sell_fk_ships FOREIGN KEY (shipid) REFERENCES ships(id);
