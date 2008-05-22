ALTER TABLE tradepost_buy_limit ADD CONSTRAINT tradepost_buy_limit_fk_ships FOREIGN KEY (shipid) REFERENCES ships(id);
