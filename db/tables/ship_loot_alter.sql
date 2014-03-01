create index shiptype on ship_loot (shiptype);
ALTER TABLE ship_loot ADD CONSTRAINT ship_loot_fk_users1 FOREIGN KEY (owner) REFERENCES users(id);
ALTER TABLE ship_loot ADD CONSTRAINT ship_loot_fk_users2 FOREIGN KEY (targetuser) REFERENCES users(id);
